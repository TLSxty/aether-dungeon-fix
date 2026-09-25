package com.xianmo.aetherfix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xianmo.aetherfix.AetherDungeonFix;
import com.xianmo.aetherfix.entity.SliderEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * S4-3 Slider 发光层（1.18.2 API）。
 * 注意：1.18.2 的 EyesLayer.renderType() 是无参抽象方法（1.19.2 才带实体参数），
 * 因此按实体切换纹理必须在 render() 内自行完成，renderType() 仅作占位实现。
 */
// 【已完成·勿改】本类已实机验证通过，逻辑请勿改动；如需调整请先确认影响面。
public class SliderGlowLayer extends EyesLayer<SliderEntity, SliderModel> {

    private static final RenderType SLIDER_AWAKE_GLOW = RenderType.eyes(
            new ResourceLocation(AetherDungeonFix.MODID, "textures/entity/mobs/slider/slider_awake_glow.png"));
    private static final RenderType SLIDER_AWAKE_CRITICAL_GLOW = RenderType.eyes(
            new ResourceLocation(AetherDungeonFix.MODID, "textures/entity/mobs/slider/slider_awake_critical_glow.png"));

    public SliderGlowLayer(RenderLayerParent<SliderEntity, SliderModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int packedLight, SliderEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!entity.isAwake()) {
            return;
        }
        RenderType type = entity.isCritical() ? SLIDER_AWAKE_CRITICAL_GLOW : SLIDER_AWAKE_GLOW;
        this.getParentModel().renderToBuffer(pose, buffer.getBuffer(type),
                packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public RenderType renderType() {
        return SLIDER_AWAKE_GLOW;
    }
}