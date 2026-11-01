package com.skyfall.airstrike.client;

import com.skyfall.airstrike.SkyfallMod;
import com.skyfall.airstrike.client.renderer.WorldTargetMarkerRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SkyfallMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientStrikeState.tickClient();
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;

        float partial = (float) event.getPartialTick();
        float amp = Mth.lerp(partial, ClientStrikeState.prevShakeAmplitude, ClientStrikeState.shakeAmplitude);
        if (amp > 0.01F) {
            float time = (mc.player.tickCount + partial) * 1.85F;
            float pitchOffset = Mth.sin(time * 2.3F) * amp * 0.45F;
            float yawOffset = Mth.cos(time * 1.9F) * amp * 0.45F;
            float rollOffset = Mth.sin(time * 3.1F) * amp * 0.65F;

            event.setPitch(event.getPitch() + pitchOffset);
            event.setYaw(event.getYaw() + yawOffset);
            event.setRoll(event.getRoll() + rollOffset);
        }
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        float flash = ClientStrikeState.dynamicFlashIntensity;
        if (flash > 0.01F) {
            if (ClientStrikeState.lastFlashWasEmp) {
                event.setRed(Mth.lerp(flash, event.getRed(), 0.35F));
                event.setGreen(Mth.lerp(flash, event.getGreen(), 0.90F));
                event.setBlue(Mth.lerp(flash, event.getBlue(), 1.0F));
            } else {
                event.setRed(Mth.lerp(flash, event.getRed(), 1.0F));
                event.setGreen(Mth.lerp(flash, event.getGreen(), 0.72F));
                event.setBlue(Mth.lerp(flash, event.getBlue(), 0.35F));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            WorldTargetMarkerRenderer.renderWorldTargetMarker(event.getPoseStack(), event.getPartialTick());
        }
    }
}
