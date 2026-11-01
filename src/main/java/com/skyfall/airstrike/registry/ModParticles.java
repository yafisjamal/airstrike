package com.skyfall.airstrike.registry;

import com.skyfall.airstrike.SkyfallMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SkyfallMod.MOD_ID);

    public static final RegistryObject<SimpleParticleType> EXPLOSION_FLASH =
            PARTICLE_TYPES.register("explosion_flash", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> SHOCKWAVE_RING =
            PARTICLE_TYPES.register("shockwave_ring", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> EXPLOSION_SMOKE_DARK =
            PARTICLE_TYPES.register("explosion_smoke_dark", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> EXPLOSION_SMOKE_LIGHT =
            PARTICLE_TYPES.register("explosion_smoke_light", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> DEBRIS_CHUNK =
            PARTICLE_TYPES.register("debris_chunk", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> EMP_ARC =
            PARTICLE_TYPES.register("emp_arc", () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
