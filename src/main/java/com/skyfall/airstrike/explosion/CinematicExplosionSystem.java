package com.skyfall.airstrike.explosion;

import com.skyfall.airstrike.config.SkyfallConfig;
import com.skyfall.airstrike.entity.StrikeDroneEntity;
import com.skyfall.airstrike.network.SkyfallNetwork;
import com.skyfall.airstrike.network.packet.S2CExplosionCinematicPacket;
import com.skyfall.airstrike.registry.ModParticles;
import com.skyfall.airstrike.registry.ModSounds;
import com.skyfall.airstrike.strike.AirstrikeMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CinematicExplosionSystem {
    private static final List<ActiveSmokeZone> ACTIVE_SMOKE_ZONES = new ArrayList<>();

    public static void detonate(ServerLevel level, StrikeDroneEntity drone, Vec3 pos, float basePower, AirstrikeMode mode) {
        float effectivePower = (float) (basePower * mode.getPowerMultiplier());
        boolean isEmp = (mode == AirstrikeMode.EMP);

        // Broadcast cinematic explosion packet to all clients within 192 blocks
        SkyfallNetwork.sendToNearby(
                level,
                pos,
                192.0D,
                new S2CExplosionCinematicPacket(pos.x, pos.y, pos.z, effectivePower, isEmp)
        );

        if (isEmp) {
            executeEmpDetonation(level, drone, pos, effectivePower);
        } else {
            executeHighExplosiveDetonation(level, drone, pos, effectivePower);
        }

        // Register lingering volumetric explosion fog zone
        int smokeDuration = SkyfallConfig.SMOKE_DURATION_TICKS.get();
        synchronized (ACTIVE_SMOKE_ZONES) {
            if (ACTIVE_SMOKE_ZONES.size() < 40) {
                ACTIVE_SMOKE_ZONES.add(new ActiveSmokeZone(level, pos, effectivePower, isEmp, smokeDuration));
            }
        }
    }

    private static void executeHighExplosiveDetonation(ServerLevel level, StrikeDroneEntity drone, Vec3 pos, float power) {
        boolean destroyBlocks = SkyfallConfig.DESTROY_BLOCKS.get();
        Level.ExplosionInteraction interaction = destroyBlocks
                ? Level.ExplosionInteraction.TNT
                : Level.ExplosionInteraction.NONE;

        // Perform core physical block/entity explosion
        level.explode(drone, pos.x, pos.y, pos.z, power, destroyBlocks, interaction);

        // Apply custom tactical blast wave damage & concussion effects
        double blastRadius = power * 3.2D;
        double dmgMultiplier = SkyfallConfig.ENTITY_DAMAGE_MULTIPLIER.get();
        AABB area = new AABB(
                pos.x - blastRadius, pos.y - blastRadius, pos.z - blastRadius,
                pos.x + blastRadius, pos.y + blastRadius, pos.z + blastRadius
        );

        List<Entity> entities = level.getEntities(drone, area);
        for (Entity entity : entities) {
            if (entity instanceof StrikeDroneEntity) continue;
            double dist = entity.position().distanceTo(pos);
            if (dist <= blastRadius) {
                double falloff = 1.0D - (dist / blastRadius);
                if (entity instanceof LivingEntity living && dmgMultiplier > 0.0D) {
                    float damage = (float) (falloff * falloff * power * 7.5D * dmgMultiplier);
                    living.hurt(level.damageSources().explosion(drone, drone), damage);
                }
                // Shockwave impulse
                Vec3 push = entity.position().subtract(pos);
                if (push.lengthSqr() > 0.001D) {
                    Vec3 impulse = push.normalize().scale(falloff * Math.min(2.2D, power * 0.25D));
                    entity.push(impulse.x, Math.min(0.85D, 0.25D + falloff * 0.45D), impulse.z);
                    entity.hurtMarked = true;
                }
            }
        }

        // Server-side multi-layered sound broadcast for distant players
        float volMult = SkyfallConfig.SOUND_VOLUME_MULTIPLIER.get().floatValue();
        level.playSound(null, pos.x, pos.y, pos.z, ModSounds.EXPLOSION_BLAST.get(),
                SoundSource.BLOCKS, 5.0F * volMult, 0.9F + level.random.nextFloat() * 0.2F);
        level.playSound(null, pos.x, pos.y, pos.z, ModSounds.EXPLOSION_BASS.get(),
                SoundSource.BLOCKS, 6.0F * volMult, 0.85F + level.random.nextFloat() * 0.15F);
    }

    private static void executeEmpDetonation(ServerLevel level, StrikeDroneEntity drone, Vec3 pos, float power) {
        float volMult = SkyfallConfig.SOUND_VOLUME_MULTIPLIER.get().floatValue();
        level.playSound(null, pos.x, pos.y, pos.z, ModSounds.EMP_BLAST.get(),
                SoundSource.BLOCKS, 5.0F * volMult, 1.0F);

        // Visual-only lightning bolt at impact center
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos.x, pos.y, pos.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        double empRadius = power * 3.8D;
        AABB area = new AABB(
                pos.x - empRadius, pos.y - empRadius, pos.z - empRadius,
                pos.x + empRadius, pos.y + empRadius, pos.z + empRadius
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity target : targets) {
            double dist = target.position().distanceTo(pos);
            if (dist <= empRadius) {
                // EMP disrupts movement, vision, and electronics/constructs
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));

                if (target instanceof IronGolem) {
                    target.hurt(level.damageSources().lightningBolt(), 35.0F);
                } else {
                    target.hurt(level.damageSources().lightningBolt(), 6.0F);
                }
            }
        }

        // Extinguish exposed flames/campfires and disrupt redstone torches in small radius
        BlockPos center = BlockPos.containing(pos);
        int blockRadius = Math.min(10, Mth.ceil(power * 1.5F));
        for (BlockPos bp : BlockPos.betweenClosed(
                center.offset(-blockRadius, -3, -blockRadius),
                center.offset(blockRadius, 4, blockRadius))) {
            BlockState state = level.getBlockState(bp);
            if (state.is(Blocks.FIRE)) {
                level.removeBlock(bp, false);
            } else if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
                level.setBlock(bp, state.setValue(CampfireBlock.LIT, false), 3);
            }
        }
    }

    public static void tickSmokeZones() {
        double density = SkyfallConfig.PARTICLE_DENSITY.get();
        synchronized (ACTIVE_SMOKE_ZONES) {
            Iterator<ActiveSmokeZone> it = ACTIVE_SMOKE_ZONES.iterator();
            while (it.hasNext()) {
                ActiveSmokeZone zone = it.next();
                zone.age++;
                if (zone.age > zone.maxAge || zone.level.isClientSide) {
                    it.remove();
                    continue;
                }

                // Spawn lingering volumetric explosion fog every 4 ticks
                if (zone.age % 4 == 0) {
                    float progress = (float) zone.age / (float) zone.maxAge;
                    double currentSpread = zone.power * (0.8D + progress * 1.8D);
                    int count = Math.max(1, Mth.ceil((1.0F - progress * 0.65F) * 5.0D * density));

                    if (zone.empMode) {
                        zone.level.sendParticles(
                                ModParticles.EMP_ARC.get(),
                                zone.pos.x, zone.pos.y + 1.0D, zone.pos.z,
                                Math.max(1, count),
                                currentSpread * 0.6D, 1.5D, currentSpread * 0.6D,
                                0.02D
                        );
                    } else {
                        // Thick dark smoke near center
                        zone.level.sendParticles(
                                ModParticles.EXPLOSION_SMOKE_DARK.get(),
                                zone.pos.x, zone.pos.y + 0.8D + progress * 2.5D, zone.pos.z,
                                count,
                                currentSpread * 0.45D, 0.8D + progress * 1.5D, currentSpread * 0.45D,
                                0.015D
                        );
                        // Lighter dust/smoke spreading outward
                        zone.level.sendParticles(
                                ModParticles.EXPLOSION_SMOKE_LIGHT.get(),
                                zone.pos.x, zone.pos.y + 0.5D + progress * 1.8D, zone.pos.z,
                                count + 2,
                                currentSpread * 0.85D, 0.6D + progress * 1.2D, currentSpread * 0.85D,
                                0.025D
                        );
                    }
                }
            }
        }
    }

    public static void clearAllSmokeZones() {
        synchronized (ACTIVE_SMOKE_ZONES) {
            ACTIVE_SMOKE_ZONES.clear();
        }
    }

    private static class ActiveSmokeZone {
        final ServerLevel level;
        final Vec3 pos;
        final float power;
        final boolean empMode;
        final int maxAge;
        int age = 0;

        ActiveSmokeZone(ServerLevel level, Vec3 pos, float power, boolean empMode, int maxAge) {
            this.level = level;
            this.pos = pos;
            this.power = power;
            this.empMode = empMode;
            this.maxAge = maxAge;
        }
    }
}
