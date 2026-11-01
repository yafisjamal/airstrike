package com.skyfall.airstrike.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class SkyfallConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue MAX_DRONE_COUNT;
    public static final ForgeConfigSpec.DoubleValue MAX_STRIKE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue DEFAULT_SPAWN_ALTITUDE;
    public static final ForgeConfigSpec.DoubleValue EXPLOSION_POWER;
    public static final ForgeConfigSpec.BooleanValue DESTROY_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue ENTITY_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_DENSITY;
    public static final ForgeConfigSpec.IntValue SMOKE_DURATION_TICKS;
    public static final ForgeConfigSpec.DoubleValue SCREEN_SHAKE_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue SOUND_VOLUME_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue WARNING_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue COOLDOWN_TICKS;

    static {
        BUILDER.push("Skyfall Tactical Airstrike Configuration");

        MAX_DRONE_COUNT = BUILDER
                .comment("Maximum number of strike UAVs allowed in a single airstrike")
                .defineInRange("maxDroneCount", 32, 1, 64);

        MAX_STRIKE_RADIUS = BUILDER
                .comment("Maximum target zone radius in blocks")
                .defineInRange("maxStrikeRadius", 64.0D, 4.0D, 256.0D);

        DEFAULT_SPAWN_ALTITUDE = BUILDER
                .comment("Height above the target Y coordinate at which Strike UAVs deploy")
                .defineInRange("defaultSpawnAltitude", 85.0D, 30.0D, 220.0D);

        EXPLOSION_POWER = BUILDER
                .comment("Base warhead explosion power (4.0 = TNT, 6.0 = Cruise Missile / Heavy UAV)")
                .defineInRange("explosionPower", 6.0D, 1.0D, 32.0D);

        DESTROY_BLOCKS = BUILDER
                .comment("Whether high-explosive airstrikes destroy terrain blocks")
                .define("destroyBlocks", true);

        ENTITY_DAMAGE_MULTIPLIER = BUILDER
                .comment("Multiplier applied to warhead blast and shrapnel damage against entities")
                .defineInRange("entityDamageMultiplier", 1.5D, 0.0D, 10.0D);

        PARTICLE_DENSITY = BUILDER
                .comment("Multiplier for cinematic explosion particles and volumetric smoke density")
                .defineInRange("particleDensity", 1.0D, 0.1D, 3.0D);

        SMOKE_DURATION_TICKS = BUILDER
                .comment("Duration in ticks (20 ticks = 1 second) that battlefield explosion fog lingers")
                .defineInRange("smokeDurationTicks", 180, 20, 600);

        SCREEN_SHAKE_INTENSITY = BUILDER
                .comment("Multiplier for camera shake intensity during nearby detonations")
                .defineInRange("screenShakeIntensity", 1.0D, 0.0D, 3.0D);

        SOUND_VOLUME_MULTIPLIER = BUILDER
                .comment("Master volume multiplier for custom airstrike audio layers")
                .defineInRange("soundVolumeMultiplier", 1.0D, 0.0D, 2.0D);

        WARNING_DURATION_TICKS = BUILDER
                .comment("Delay in ticks between target lock warning siren and UAV wave deployment")
                .defineInRange("warningDurationTicks", 50, 0, 200);

        COOLDOWN_TICKS = BUILDER
                .comment("Cooldown in ticks between Strike Designator activations for non-creative players")
                .defineInRange("cooldownTicks", 160, 0, 6000);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
