package com.skyfall.airstrike.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.skyfall.airstrike.SkyfallMod;
import com.skyfall.airstrike.entity.StrikeDroneEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class StrikeDroneModel extends EntityModel<StrikeDroneEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(SkyfallMod.MOD_ID, "strike_drone"), "main");

    private final ModelPart root;
    private final ModelPart propeller;
    private final ModelPart beaconLed;

    public StrikeDroneModel(ModelPart root) {
        this.root = root.getChild("root");
        this.propeller = this.root.getChild("propeller");
        this.beaconLed = this.root.getChild("beacon_led");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();

        PartDefinition root = partRoot.addOrReplaceChild("root", CubeListBuilder.create()
                // Main stealth UAV fuselage
                .texOffs(0, 0).addBox(-2.5F, -2.0F, -8.0F, 5.0F, 4.0F, 16.0F, new CubeDeformation(0.0F))
                // Tapered forward warhead / avionics nosecone
                .texOffs(0, 4).addBox(-1.5F, -1.5F, -12.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                // Dorsal satellite uplink spine
                .texOffs(0, 44).addBox(-1.0F, -3.0F, -4.0F, 2.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
                // Rear pusher-propeller shroud
                .texOffs(16, 44).addBox(-1.5F, -1.5F, 8.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 20.0F, 0.0F));

        // Swept tactical delta wings
        root.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(0, 24).addBox(2.0F, -0.5F, -3.5F, 13.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                // Under-wing micro-munition pylon
                .texOffs(32, 44).addBox(5.5F, 0.5F, -3.0F, 1.5F, 1.5F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.18F, 0.0F));

        root.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-15.0F, -0.5F, -3.5F, 13.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                // Under-wing micro-munition pylon
                .texOffs(32, 52).addBox(-7.0F, 0.5F, -3.0F, 1.5F, 1.5F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.18F, 0.0F));

        // V-Tail stabilizers (MQ-9 Reaper / Switchblade style)
        root.addOrReplaceChild("left_tail", CubeListBuilder.create()
                .texOffs(0, 46).addBox(0.5F, -5.0F, 4.5F, 1.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.15F, 0.0F, 0.55F));

        root.addOrReplaceChild("right_tail", CubeListBuilder.create()
                .texOffs(12, 46).addBox(-1.5F, -5.0F, 4.5F, 1.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.15F, 0.0F, -0.55F));

        // 4-Blade high-speed rear pusher propeller
        root.addOrReplaceChild("propeller", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-5.0F, -0.5F, 0.0F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(48, 4).addBox(-0.5F, -5.0F, 0.0F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 10.2F));

        // Blinking Red Optical Target Seeker & Dorsal Beacon LED
        root.addOrReplaceChild("beacon_led", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-1.0F, 0.5F, -11.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.15F))
                .texOffs(32, 0).addBox(-0.5F, -3.8F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(StrikeDroneEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float partialTicks = ageInTicks - entity.tickCount;
        float spinDeg = Mth.lerp(partialTicks, entity.prevPropellerAngle, entity.propellerAngle);
        this.propeller.zRot = spinDeg * ((float) Math.PI / 180.0F);
    }

    public void renderBeacon(PoseStack poseStack, VertexConsumer buffer, int packedOverlay) {
        this.root.translateAndRotate(poseStack);
        this.beaconLed.render(poseStack, buffer, 15728880, packedOverlay, 1.0F, 0.2F, 0.2F, 1.0F);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
