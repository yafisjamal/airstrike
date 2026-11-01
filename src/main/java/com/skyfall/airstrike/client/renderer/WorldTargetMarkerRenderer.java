package com.skyfall.airstrike.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.skyfall.airstrike.client.ClientStrikeState;
import com.skyfall.airstrike.item.StrikeDesignatorItem;
import com.skyfall.airstrike.strike.AirstrikeMode;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class WorldTargetMarkerRenderer {

    public static void renderWorldTargetMarker(PoseStack poseStack, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        Vec3 markerPos = null;
        float radius = 8.0F;
        float r = 0.15F, g = 1.0F, b = 0.45F;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack designator = main.getItem() instanceof StrikeDesignatorItem ? main
                : (off.getItem() instanceof StrikeDesignatorItem ? off : ItemStack.EMPTY);

        if (!designator.isEmpty()) {
            BlockHitResult hit = StrikeDesignatorItem.raycastTarget(mc.level, player, StrikeDesignatorItem.MAX_TARGET_RANGE);
            if (hit.getType() != HitResult.Type.MISS) {
                markerPos = Vec3.atBottomCenterOf(hit.getBlockPos().above());
                radius = StrikeDesignatorItem.getRadius(designator);
                AirstrikeMode mode = StrikeDesignatorItem.getMode(designator);
                int col = mode.getHudColor();
                r = ((col >> 16) & 0xFF) / 255.0F;
                g = ((col >> 8) & 0xFF) / 255.0F;
                b = (col & 0xFF) / 255.0F;
            }
        }

        if (ClientStrikeState.strikePhase == 1 || ClientStrikeState.strikePhase == 2) {
            markerPos = ClientStrikeState.activeTargetPos;
            radius = ClientStrikeState.activeRadius;
            r = 1.0F;
            g = 0.22F;
            b = 0.18F;
        }

        if (markerPos == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(markerPos.x - camPos.x, markerPos.y - camPos.y + 0.08D, markerPos.z - camPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer lineBuffer = bufferSource.getBuffer(RenderType.lines());

        float time = (mc.level.getGameTime() + partialTick) * 0.06F;

        // 1. Outer Strike Radius Perimeter Ring
        drawHorizontalCircle(poseStack, lineBuffer, radius, 48, 0.0F, r, g, b, 0.90F);
        // 2. Animated Pulsing Inner Ring
        float pulseRadius = radius * (0.25F + 0.70F * ((time * 0.65F) % 1.0F));
        drawHorizontalCircle(poseStack, lineBuffer, pulseRadius, 36, 0.05F, r, g, b, 0.55F);
        // 3. Target Center Crosshairs & Vertical Orbital Designator Beam
        drawLine(poseStack, lineBuffer, -2.5F, 0.05F, 0.0F, 2.5F, 0.05F, 0.0F, r, g, b, 0.95F);
        drawLine(poseStack, lineBuffer, 0.0F, 0.05F, -2.5F, 0.0F, 0.05F, 2.5F, r, g, b, 0.95F);
        drawLine(poseStack, lineBuffer, 0.0F, 0.0F, 0.0F, 0.0F, 45.0F, 0.0F, r, g, b, 0.65F);

        bufferSource.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static void drawHorizontalCircle(PoseStack poseStack, VertexConsumer consumer,
                                             float radius, int segments, float yOffset,
                                             float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float a1 = (float) (2.0D * Math.PI * i / segments);
            float a2 = (float) (2.0D * Math.PI * (i + 1) / segments);
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            float x2 = Mth.cos(a2) * radius;
            float z2 = Mth.sin(a2) * radius;
            drawLine(poseStack, consumer, x1, yOffset, z1, x2, yOffset, z2, r, g, b, a);
        }
    }

    private static void drawLine(PoseStack poseStack, VertexConsumer consumer,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float r, float g, float b, float a) {
        Matrix4f mat = poseStack.last().pose();
        Matrix3f norm = poseStack.last().normal();
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 1.0E-4F) {
            dx /= len;
            dy /= len;
            dz /= len;
        } else {
            dx = 0.0F;
            dy = 1.0F;
            dz = 0.0F;
        }
        consumer.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(norm, dx, dy, dz).endVertex();
        consumer.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(norm, dx, dy, dz).endVertex();
    }
}
