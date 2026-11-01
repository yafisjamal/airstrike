package com.skyfall.airstrike;

import com.mojang.logging.LogUtils;
import com.skyfall.airstrike.config.SkyfallConfig;
import com.skyfall.airstrike.network.SkyfallNetwork;
import com.skyfall.airstrike.registry.ModCreativeTabs;
import com.skyfall.airstrike.registry.ModEntities;
import com.skyfall.airstrike.registry.ModItems;
import com.skyfall.airstrike.registry.ModParticles;
import com.skyfall.airstrike.registry.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(SkyfallMod.MOD_ID)
public class SkyfallMod {
    public static final String MOD_ID = "skyfall";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SkyfallMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModSounds.register(modEventBus);
        ModParticles.register(modEventBus);
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SkyfallConfig.SPEC, "skyfall-common.toml");

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            SkyfallNetwork.register();
            LOGGER.info("SKYFALL Tactical Airstrike System initialized.");
        });
    }
}
