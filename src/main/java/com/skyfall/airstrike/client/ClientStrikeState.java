package com.skyfall.airstrike.client;

import com.skyfall.airstrike.config.SkyfallConfig;
import com.skyfall.airstrike.registry.ModSounds;
import com.skyfall.airstrike.strike.AirstrikeMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClientStrikeState {
    // Active HUD Telemetry
    public static int strikePhase = 0; // 0=IDLE, 1=WARNING, 2=INBOUND, 3=COMPLETE
    public static AirstrikeMode activeMode = AirstrikeMode.CLUSTER;
    public static Vec3 activeTargetPos = Vec3.ZERO;
    public static float activeRadius = 14.0F;
    public static int dronesRemaining = 0;
    public static int totalDrones = 0;
    public static int warningCountdownTicks = 0;
    public static long lastStatusUpdateTick = 0L;

    // Cinematic Screen Shake & Dynamic Sky Flash
    public static float shakeAmplitude = 0.0F;
    public static float prevShakeAmplitude = 0.0F;
    public static float dynamicFlashIntensity = 0.0F;
    public static boolean lastFlashWasEmp = false;

    private static final List<DelayedAudioEvent> AUDIO_QUEUE = new ArrayList<>();

    public static void triggerCameraShakeAndFlash(Vec3 impactPos, float power, boolean empMode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double dist = mc.player.position().distanceTo(impactPos);
        double maxShakeRange = Math.max(48.0D, power * 14.0D);
        if (dist < maxShakeRange) {
            float falloff = (float) (1.0D - (dist / maxShakeRange));
            float cfgMult = SkyfallConfig.SCREEN_SHAKE_INTENSITY.get().floatValue();
            float addedShake = falloff * falloff * power * 1.45F * cfgMult;
            shakeAmplitude = Math.min(14.0F, shakeAmplitude + addedShake);
            dynamicFlashIntensity = Math.min(1.0F, dynamicFlashIntensity + falloff * 0.85F);
            lastFlashWasEmp = empMode;
        }
    }

    public static void scheduleLayeredExplosionAudio(double x, double y, double z, float power, boolean empMode) {
        float volMult = SkyfallConfig.SOUND_VOLUME_MULTIPLIER.get().floatValue();
        synchronized (AUDIO_QUEUE) {
            if (!empMode) {
                // Layer 3: Delayed environmental rumble (+10 ticks)
                AUDIO_QUEUE.add(new DelayedAudioEvent(10, x, y, z,
                        ModSounds.EXPLOSION_RUMBLE.get(), 5.5F * volMult, 0.90F));
                // Layer 4a: Falling debris (+18 ticks)
                AUDIO_QUEUE.add(new DelayedAudioEvent(18, x, y, z,
                        ModSounds.DEBRIS_FALL.get(), 3.8F * volMult, 1.0F));
                // Layer 4b: Lingering fire & smoke ambience (+28 ticks)
                AUDIO_QUEUE.add(new DelayedAudioEvent(28, x, y, z,
                        ModSounds.FIRE_AMBIENCE.get(), 3.2F * volMult, 0.95F));
            } else {
                AUDIO_QUEUE.add(new DelayedAudioEvent(12, x, y, z,
                        ModSounds.EXPLOSION_RUMBLE.get(), 3.5F * volMult, 1.25F));
            }
        }
    }

    public static void tickClient() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;

        prevShakeAmplitude = shakeAmplitude;
        shakeAmplitude = Mth.clamp(shakeAmplitude * 0.84F - 0.05F, 0.0F, 14.0F);
        dynamicFlashIntensity = Math.max(0.0F, dynamicFlashIntensity * 0.78F - 0.02F);

        if (strikePhase > 0 && level.getGameTime() - lastStatusUpdateTick > 140L) {
            strikePhase = 0;
        }

        synchronized (AUDIO_QUEUE) {
            Iterator<DelayedAudioEvent> it = AUDIO_QUEUE.iterator();
            while (it.hasNext()) {
                DelayedAudioEvent ev = it.next();
                ev.ticksRemaining--;
                if (ev.ticksRemaining <= 0) {
                    level.playLocalSound(ev.x, ev.y, ev.z, ev.sound, SoundSource.BLOCKS, ev.volume, ev.pitch, false);
                    it.remove();
                }
            }
        }
    }

    private static class DelayedAudioEvent {
        int ticksRemaining;
        final double x, y, z;
        final SoundEvent sound;
        final float volume;
        final float pitch;

        DelayedAudioEvent(int ticksRemaining, double x, double y, double z,
                          SoundEvent sound, float volume, float pitch) {
            this.ticksRemaining = ticksRemaining;
            this.x = x;
            this.y = y;
            this.z = z;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}
