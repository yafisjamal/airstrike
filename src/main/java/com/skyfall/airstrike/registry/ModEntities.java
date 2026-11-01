package com.skyfall.airstrike.registry;

import com.skyfall.airstrike.SkyfallMod;
import com.skyfall.airstrike.entity.StrikeDroneEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SkyfallMod.MOD_ID);

    public static final RegistryObject<EntityType<StrikeDroneEntity>> STRIKE_DRONE =
            ENTITY_TYPES.register("strike_drone", () ->
                    EntityType.Builder.<StrikeDroneEntity>of(StrikeDroneEntity::new, MobCategory.MISC)
                            .sized(1.1F, 0.45F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .fireImmune()
                            .build(new ResourceLocation(SkyfallMod.MOD_ID, "strike_drone").toString()));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
