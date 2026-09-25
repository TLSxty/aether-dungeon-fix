package com.xianmo.aetherfix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xianmo.aetherfix.entity.SliderEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * S4-2 Slider 模型（1.18.2 API）。
 * 依据 1.19.2 javap 结构：单部件 slider，32x32x32，纹理 128x64，轴心 (0,24,0)。
 */
// 【已完成·勿改】本类已实机验证通过，逻辑请勿改动；如需调整请先确认影响面。
public class SliderModel extends EntityModel<SliderEntity> {

    public final ModelPart slider;

    public SliderModel(ModelPart root) {
        this.slider = root.getChild("slider");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("slider",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-16.0F, -32.0F, -16.0F, 32, 32, 32, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(SliderEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buf, int light, int overlay,
                               float r, float g, float b, float a) {
        this.slider.render(pose, buf, light, overlay, r, g, b, a);
    }
}