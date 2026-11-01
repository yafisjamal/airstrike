package com.skyfall.airstrike.client;

import com.skyfall.airstrike.SkyfallMod;
import com.skyfall.airstrike.client.hud.TacticalHudOverlay;
import com.skyfall.airstrike.client.model.StrikeDroneModel;
import com.skyfall.airstrike.client.particle.DebrisChunkParticle;
import com.skyfall.airstrike.client.particle.EmpArcParticle;
import com.skyfall.airstrike.client.particle.ExplosionFlashParticle;
import com.skyfall.airstrike.client.particle.ShockwaveRingParticle;
import com.skyfall.airstrike.client.particle.VolumetricSmokeParticle;
import com.skyfall.airstrike.client.renderer.StrikeDroneRenderer;
import com.skyfall.airstrike.registry.ModEntities;
import com.skyfall.airstrike.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SkyfallMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StrikeDroneModel.LAYER_LOCATION, StrikeDroneModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.STRIKE_DRONE.get(), StrikeDroneRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.EXPLOSION_FLASH.get(), ExplosionFlashParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SHOCKWAVE_RING.get(), ShockwaveRingParticle.Provider::new);
        event.registerSpriteSet(ModParticles.EXPLOSION_SMOKE_DARK.get(), VolumetricSmokeParticle.DarkProvider::new);
        event.registerSpriteSet(ModParticles.EXPLOSION_SMOKE_LIGHT.get(), VolumetricSmokeParticle.LightProvider::new);
        event.registerSpriteSet(ModParticles.DEBRIS_CHUNK.get(), DebrisChunkParticle.Provider::new);
        event.registerSpriteSet(ModParticles.EMP_ARC.get(), EmpArcParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "skyfall_tactical_hud", new TacticalHudOverlay());
    }
}
