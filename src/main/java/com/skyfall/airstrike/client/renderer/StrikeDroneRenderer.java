package com.skyfall.airstrike.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.skyfall.airstrike.SkyfallMod;
import com.skyfall.airstrike.client.model.StrikeDroneModel;
import com.skyfall.airstrike.entity.StrikeDroneEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class StrikeDroneRenderer extends EntityRenderer<StrikeDroneEntity> {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(SkyfallMod.MOD_ID, "textures/entity/strike_drone.png");

    private final StrikeDroneModel model;

    public StrikeDroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new StrikeDroneModel(context.bakeLayer(StrikeDroneModel.LAYER_LOCATION));
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(StrikeDroneEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        float roll = Mth.lerp(partialTicks, entity.prevRollAngle, entity.rollAngle);

        poseStack.translate(0.0D, 1.25D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);

        VertexConsumer hullBuffer = buffer.getBuffer(this.model.renderType(TEXTURE));
        this.model.renderToBuffer(poseStack, hullBuffer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        // Render full-bright blinking red targeting LED
        if (entity.isTargetingLedLit()) {
            poseStack.pushPose();
            VertexConsumer glowBuffer = buffer.getBuffer(RenderType.eyes(TEXTURE));
            this.model.renderBeacon(poseStack, glowBuffer, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(StrikeDroneEntity entity) {
        return TEXTURE;
    }
}
