package com.skyfall.airstrike.strike;

import com.skyfall.airstrike.config.SkyfallConfig;
import com.skyfall.airstrike.entity.StrikeDroneEntity;
import com.skyfall.airstrike.explosion.CinematicExplosionSystem;
import com.skyfall.airstrike.network.SkyfallNetwork;
import com.skyfall.airstrike.network.packet.S2CAirstrikeStatusPacket;
import com.skyfall.airstrike.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AirstrikeManager {
    private static final List<ActiveStrikeSession> ACTIVE_SESSIONS = new ArrayList<>();
    private static final Map<UUID, PlayerStrikeSettings> PLAYER_SETTINGS = new ConcurrentHashMap<>();

    public static PlayerStrikeSettings getSettings(UUID playerUUID) {
        return PLAYER_SETTINGS.computeIfAbsent(playerUUID, k -> new PlayerStrikeSettings());
    }

    public static void initiateStrike(ServerLevel level, ServerPlayer commander, Vec3 targetCenter,
                                      AirstrikeMode mode, float radius, int droneCount,
                                      double spawnAltitude, float explosionPower) {
        int clampedDrones = Mth.clamp(droneCount, 1, SkyfallConfig.MAX_DRONE_COUNT.get());
        float clampedRadius = (float) Mth.clamp(radius, 1.0D, SkyfallConfig.MAX_STRIKE_RADIUS.get());
        int warningTicks = SkyfallConfig.WARNING_DURATION_TICKS.get();

        // Step 1-3: Target lock confirmation sound
        float volMult = SkyfallConfig.SOUND_VOLUME_MULTIPLIER.get().floatValue();
        if (commander != null) {
            level.playSound(null, commander.getX(), commander.getY(), commander.getZ(),
                    ModSounds.TARGET_LOCK.get(), SoundSource.PLAYERS, 1.5F * volMult, 1.0F);
        }

        // Step 4: Broadcast incoming airstrike warning siren at target zone
        level.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                ModSounds.AIRSTRIKE_WARNING.get(), SoundSource.RECORDS, 4.5F * volMult, 1.0F);

        ActiveStrikeSession session = new ActiveStrikeSession(
                level,
                commander != null ? commander.getUUID() : null,
                targetCenter,
                mode,
                clampedRadius,
                clampedDrones,
                spawnAltitude,
                explosionPower,
                warningTicks
        );

        synchronized (ACTIVE_SESSIONS) {
            ACTIVE_SESSIONS.add(session);
        }

        session.broadcastHudStatus(1); // Phase 1: WARNING / LOCKED
    }

    public static void tickServer() {
        CinematicExplosionSystem.tickSmokeZones();

        synchronized (ACTIVE_SESSIONS) {
            Iterator<ActiveStrikeSession> it = ACTIVE_SESSIONS.iterator();
            while (it.hasNext()) {
                ActiveStrikeSession session = it.next();
                if (session.tick()) {
                    it.remove();
                }
            }
        }
    }

    public static int cancelAllStrikes(ServerLevel level) {
        int cancelled = 0;
        synchronized (ACTIVE_SESSIONS) {
            for (ActiveStrikeSession session : ACTIVE_SESSIONS) {
                session.broadcastHudStatus(0);
                cancelled++;
            }
            ACTIVE_SESSIONS.clear();
        }
        // Remove all active StrikeDroneEntity instances in the level
        List<StrikeDroneEntity> drones = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof StrikeDroneEntity drone) {
                drones.add(drone);
            }
        }
        for (StrikeDroneEntity drone : drones) {
            drone.discard();
        }
        CinematicExplosionSystem.clearAllSmokeZones();
        return cancelled + drones.size();
    }

    public static class PlayerStrikeSettings {
        public AirstrikeMode mode = AirstrikeMode.CLUSTER;
        public float radius = AirstrikeMode.CLUSTER.getDefaultRadius();
        public int droneCount = AirstrikeMode.CLUSTER.getDefaultDrones();
        public double spawnAltitude = 85.0D;
        public float explosionPower = 6.0F;

        public void applyModeDefaults(AirstrikeMode newMode) {
            this.mode = newMode;
            this.radius = newMode.getDefaultRadius();
            this.droneCount = newMode.getDefaultDrones();
        }
    }

    private static class ActiveStrikeSession {
        private final ServerLevel level;
        private final UUID commanderId;
        private final Vec3 targetCenter;
        private final AirstrikeMode mode;
        private final float radius;
        private final int totalDrones;
        private final double spawnAltitude;
        private final float explosionPower;
        private final List<Vec3> plannedTargets;
        private final List<StrikeDroneEntity> activeDrones = new ArrayList<>();
        private final double approachAngleRad;

        private int warningTicksRemaining;
        private int spawnTimer = 0;
        private int dronesSpawned = 0;
        private int completionDisplayTicks = 0;
        private boolean completionAnnounced = false;

        ActiveStrikeSession(ServerLevel level, UUID commanderId, Vec3 targetCenter, AirstrikeMode mode,
                            float radius, int totalDrones, double spawnAltitude, float explosionPower,
                            int warningTicks) {
            this.level = level;
            this.commanderId = commanderId;
            this.targetCenter = targetCenter;
            this.mode = mode;
            this.radius = radius;
            this.totalDrones = totalDrones;
            this.spawnAltitude = spawnAltitude;
            this.explosionPower = explosionPower;
            this.warningTicksRemaining = warningTicks;
            this.approachAngleRad = level.random.nextDouble() * Math.PI * 2.0D;
            this.plannedTargets = computeTargetPattern(level.random);
        }

        private List<Vec3> computeTargetPattern(RandomSource rand) {
            List<Vec3> list = new ArrayList<>(totalDrones);
            double dirX = Math.cos(approachAngleRad);
            double dirZ = Math.sin(approachAngleRad);
            double perpX = -dirZ;
            double perpZ = dirX;

            for (int i = 0; i < totalDrones; i++) {
                switch (mode) {
                    case PRECISION -> {
                        double jitter = (totalDrones == 1) ? 0.0D : 1.5D;
                        double ox = (rand.nextDouble() - 0.5D) * jitter;
                        double oz = (rand.nextDouble() - 0.5D) * jitter;
                        list.add(targetCenter.add(ox, 0.0D, oz));
                    }
                    case CLUSTER -> {
                        if (i == 0) {
                            list.add(targetCenter);
                        } else {
                            double angle = (2.0D * Math.PI * i) / Math.max(1, totalDrones - 1) + (rand.nextDouble() - 0.5D) * 0.35D;
                            double dist = radius * (0.45D + 0.45D * rand.nextDouble());
                            list.add(targetCenter.add(Math.cos(angle) * dist, 0.0D, Math.sin(angle) * dist));
                        }
                    }
                    case CARPET -> {
                        // Coordinated rolling line / dual-column grid along approach vector
                        double t = (totalDrones <= 1) ? 0.0D : ((double) i / (double) (totalDrones - 1)) * 2.0D - 1.0D;
                        double along = t * radius;
                        double lane = ((i % 2 == 0) ? 1.0D : -1.0D) * Math.min(4.5D, radius * 0.22D);
                        double ox = dirX * along + perpX * lane;
                        double oz = dirZ * along + perpZ * lane;
                        list.add(targetCenter.add(ox, 0.0D, oz));
                    }
                    case SUPPRESSION -> {
                        double r = radius * Math.sqrt(rand.nextDouble());
                        double theta = rand.nextDouble() * Math.PI * 2.0D;
                        list.add(targetCenter.add(Math.cos(theta) * r, 0.0D, Math.sin(theta) * r));
                    }
                    case EMP -> {
                        if (i == 0) {
                            list.add(targetCenter);
                        } else {
                            double angle = (2.0D * Math.PI * i) / totalDrones;
                            double dist = radius * 0.55D;
                            list.add(targetCenter.add(Math.cos(angle) * dist, 0.0D, Math.sin(angle) * dist));
                        }
                    }
                }
            }
            return list;
        }

        boolean tick() {
            // Phase 1: Warning Countdown
            if (warningTicksRemaining > 0) {
                warningTicksRemaining--;
                if (warningTicksRemaining % 5 == 0 || warningTicksRemaining == 0) {
                    broadcastHudStatus(1);
                }
                return false;
            }

            // Phase 2: Drone Deployment Wave
            if (dronesSpawned < totalDrones) {
                if (spawnTimer <= 0) {
                    spawnNextDrone(dronesSpawned);
                    dronesSpawned++;
                    spawnTimer = mode.getSpawnIntervalTicks();
                    broadcastHudStatus(2);
                } else {
                    spawnTimer--;
                }
            }

            // Clean up detonated drones
            activeDrones.removeIf(d -> !d.isAlive() || d.isRemoved());

            if (dronesSpawned < totalDrones || !activeDrones.isEmpty()) {
                if (level.getGameTime() % 10 == 0) {
                    broadcastHudStatus(2);
                }
                return false;
            }

            // Phase 3: Final Airstrike Completion Notification
            if (!completionAnnounced) {
                completionAnnounced = true;
                completionDisplayTicks = 50;
                broadcastHudStatus(3);
                if (commanderId != null) {
                    ServerPlayer player = level.getServer().getPlayerList().getPlayer(commanderId);
                    if (player != null) {
                        player.displayClientMessage(
                                Component.literal("[SKYFALL] ")
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                                        .append(Component.literal(mode.getDisplayName() + " COMPLETE — GOOD EFFECT ON TARGET.")
                                                .withStyle(ChatFormatting.GREEN)),
                                true
                        );
                    }
                }
            }

            if (completionDisplayTicks > 0) {
                completionDisplayTicks--;
                return false;
            }

            broadcastHudStatus(0);
            return true;
        }

        private void spawnNextDrone(int index) {
            Vec3 impactTarget = plannedTargets.get(index);
            // Spawn high above target at an angled tactical approach offset so UAVs swoop in cinematically
            double offsetX = -Math.cos(approachAngleRad) * doubleStandoff(spawnAltitude)
                    + (level.random.nextDouble() - 0.5D) * 6.0D;
            double offsetZ = -Math.sin(approachAngleRad) * doubleStandoff(spawnAltitude)
                    + (level.random.nextDouble() - 0.5D) * 6.0D;

            Vec3 spawnPos = new Vec3(
                    impactTarget.x + offsetX,
                    impactTarget.y + spawnAltitude + level.random.nextDouble() * 8.0D,
                    impactTarget.z + offsetZ
            );

            StrikeDroneEntity drone = new StrikeDroneEntity(
                    level,
                    spawnPos,
                    impactTarget,
                    mode,
                    explosionPower,
                    commanderId
            );
            level.addFreshEntity(drone);
            activeDrones.add(drone);
        }

        private double doubleStandoff(double alt) {
            return alt * 0.38D;
        }

        void broadcastHudStatus(int phase) {
            int remaining = (totalDrones - dronesSpawned) + activeDrones.size();
            S2CAirstrikeStatusPacket pkt = new S2CAirstrikeStatusPacket(
                    phase,
                    mode.getId(),
                    targetCenter.x,
                    targetCenter.y,
                    targetCenter.z,
                    radius,
                    remaining,
                    totalDrones,
                    warningTicksRemaining
            );
            SkyfallNetwork.sendToNearby(level, targetCenter, 192.0D, pkt);
            if (commanderId != null) {
                ServerPlayer commander = level.getServer().getPlayerList().getPlayer(commanderId);
                if (commander != null && commander.position().distanceToSqr(targetCenter) > 192.0D * 192.0D) {
                    SkyfallNetwork.sendToPlayer(commander, pkt);
                }
            }
        }
    }
}
