package com.skyfall.airstrike.client;

import com.skyfall.airstrike.config.SkyfallConfig;
import com.skyfall.airstrike.registry.ModParticles;
import com.skyfall.airstrike.registry.ModSounds;
import com.skyfall.airstrike.strike.AirstrikeMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class ClientPacketHandlers {

    public static void handleAirstrikeStatus(int phase, String modeId, double tx, double ty, double tz,
                                             float radius, int dronesRemaining, int totalDrones, int countdownTicks) {
        Minecraft mc = Minecraft.getInstance();
        ClientStrikeState.strikePhase = phase;
        ClientStrikeState.activeMode = AirstrikeMode.byId(modeId);
        ClientStrikeState.activeTargetPos = new Vec3(tx, ty, tz);
        ClientStrikeState.activeRadius = radius;
        ClientStrikeState.dronesRemaining = dronesRemaining;
        ClientStrikeState.totalDrones = totalDrones;
        ClientStrikeState.warningCountdownTicks = countdownTicks;
        if (mc.level != null) {
            ClientStrikeState.lastStatusUpdateTick = mc.level.getGameTime();
        }
    }

    public static void handleExplosionCinematic(double x, double y, double z, float power, boolean empMode) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Vec3 impactPos = new Vec3(x, y, z);
        RandomSource rand = level.random;
        double density = SkyfallConfig.PARTICLE_DENSITY.get();
        float volMult = SkyfallConfig.SOUND_VOLUME_MULTIPLIER.get().floatValue();

        // 1. Trigger camera shake & dynamic sky illumination flash
        ClientStrikeState.triggerCameraShakeAndFlash(impactPos, power, empMode);

        // 2. Play immediate Layer 1 & Layer 2 local sounds and queue Layer 3 & Layer 4
        if (empMode) {
            level.playLocalSound(x, y, z, ModSounds.EMP_BLAST.get(), SoundSource.BLOCKS, 5.0F * volMult, 1.0F, false);
        } else {
            level.playLocalSound(x, y, z, ModSounds.EXPLOSION_BLAST.get(), SoundSource.BLOCKS, 5.2F * volMult, 0.95F, false);
            level.playLocalSound(x, y, z, ModSounds.EXPLOSION_BASS.get(), SoundSource.BLOCKS, 6.0F * volMult, 0.88F, false);
        }
        ClientStrikeState.scheduleLayeredExplosionAudio(x, y, z, power, empMode);

        // 3. Central blinding flash particle
        level.addAlwaysVisibleParticle(ModParticles.EXPLOSION_FLASH.get(), true, x, y + 0.6D, z, power, 0.0D, 0.0D);

        // 4. Expanding horizontal shockwave ring
        int ringSegments = Mth.ceil(28 * density);
        for (int i = 0; i < ringSegments; i++) {
            double angle = (2.0D * Math.PI * i) / ringSegments;
            double speed = (0.45D + power * 0.08D) * (0.9D + rand.nextDouble() * 0.2D);
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            level.addAlwaysVisibleParticle(
                    empMode ? ModParticles.EMP_ARC.get() : ModParticles.SHOCKWAVE_RING.get(),
                    true,
                    x + vx * 0.5D, y + 0.35D, z + vz * 0.5D,
                    vx, 0.01D, vz
            );
        }

        if (empMode) {
            // EMP plasma sphere burst
            int empCount = Mth.ceil(45 * density);
            for (int i = 0; i < empCount; i++) {
                double rx = (rand.nextDouble() - 0.5D) * power * 2.2D;
                double ry = rand.nextDouble() * power * 1.5D;
                double rz = (rand.nextDouble() - 0.5D) * power * 2.2D;
                level.addAlwaysVisibleParticle(ModParticles.EMP_ARC.get(), true,
                        x + rx, y + ry, z + rz, rx * 0.1D, 0.05D, rz * 0.1D);
            }
            return;
        }

        // 5. Dense dark pyroclastic core smoke column
        int darkSmokeCount = Mth.ceil(36 * density);
        for (int i = 0; i < darkSmokeCount; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0D;
            double hSpeed = rand.nextDouble() * (0.18D + power * 0.03D);
            double vy = 0.04D + rand.nextDouble() * 0.16D;
            level.addAlwaysVisibleParticle(
                    ModParticles.EXPLOSION_SMOKE_DARK.get(),
                    true,
                    x + (rand.nextDouble() - 0.5D) * 2.2D,
                    y + 0.4D + rand.nextDouble() * 2.0D,
                    z + (rand.nextDouble() - 0.5D) * 2.2D,
                    Math.cos(angle) * hSpeed, vy, Math.sin(angle) * hSpeed
            );
        }

        // 6. Outer ground dust clouds & lighter volumetric fog
        int lightSmokeCount = Mth.ceil(42 * density);
        for (int i = 0; i < lightSmokeCount; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0D;
            double hSpeed = 0.12D + rand.nextDouble() * (0.32D + power * 0.04D);
            double vy = 0.02D + rand.nextDouble() * 0.10D;
            level.addAlwaysVisibleParticle(
                    ModParticles.EXPLOSION_SMOKE_LIGHT.get(),
                    true,
                    x + Math.cos(angle) * 1.5D,
                    y + 0.25D + rand.nextDouble() * 1.2D,
                    z + Math.sin(angle) * 1.5D,
                    Math.cos(angle) * hSpeed, vy, Math.sin(angle) * hSpeed
            );
        }

        // 7. High-velocity flying debris & shrapnel chunks
        int debrisCount = Mth.ceil(34 * density);
        for (int i = 0; i < debrisCount; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0D;
            double speed = 0.25D + rand.nextDouble() * (0.55D + power * 0.06D);
            double vy = 0.35D + rand.nextDouble() * 0.65D;
            level.addAlwaysVisibleParticle(
                    ModParticles.DEBRIS_CHUNK.get(),
                    true,
                    x, y + 0.5D, z,
                    Math.cos(angle) * speed, vy, Math.sin(angle) * speed
            );
        }

        // 8. Fireball sparks & embers
        int sparkCount = Mth.ceil(28 * density);
        for (int i = 0; i < sparkCount; i++) {
            double vx = (rand.nextDouble() - 0.5D) * 0.85D;
            double vy = 0.15D + rand.nextDouble() * 0.55D;
            double vz = (rand.nextDouble() - 0.5D) * 0.85D;
            level.addParticle(ParticleTypes.FLAME, x, y + 0.6D, z, vx, vy, vz);
            if (i % 2 == 0) {
                level.addParticle(ParticleTypes.LAVA, x, y + 0.6D, z, vx * 0.5D, vy, vz * 0.5D);
            }
        }
    }
}
