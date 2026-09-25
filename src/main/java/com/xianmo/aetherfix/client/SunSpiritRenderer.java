package com.xianmo.aetherfix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xianmo.aetherfix.AetherDungeonFix;
import com.xianmo.aetherfix.entity.SunSpiritEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * 烈阳巨灵渲染器（B5）。
 *
 * 对齐 1.19.2 SunSpiritRenderer：
 * - 贴图按 isFrozen() 二选一（正常 / 冰冻）
 * - scale 2.25 倍 + 抬高 0.85
 * - 方块光 / 天空光 恒为 15（自发光）
 */
public class SunSpiritRenderer extends MobRenderer<SunSpiritEntity, SunSpiritModel<SunSpiritEntity>> {

    public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER =
            new net.minecraft.client.model.geom.ModelLayerLocation(
                    new ResourceLocation(AetherDungeonFix.MODID, "sun_spirit"), "main");

    private static final ResourceLocation SUN_SPIRIT_TEXTURE =
            new ResourceLocation(AetherDungeonFix.MODID, "textures/entity/mobs/sun_spirit/sun_spirit.png");
    private static final ResourceLocation FROZEN_SPIRIT_TEXTURE =
            new ResourceLocation(AetherDungeonFix.MODID, "textures/entity/mobs/sun_spirit/frozen_sun_spirit.png");

    public SunSpiritRenderer(EntityRendererProvider.Context context) {
        super(context, new SunSpiritModel<>(context.bakeLayer(LAYER)), 0.8F);
    }

    @Override
    protected void scale(SunSpiritEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(2.25F, 2.25F, 2.25F);
        poseStack.translate(0.0, 0.85, 0.0);
    }

    @Override
    public ResourceLocation getTextureLocation(SunSpiritEntity entity) {
        return entity.isFrozen() ? FROZEN_SPIRIT_TEXTURE : SUN_SPIRIT_TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(SunSpiritEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    protected int getSkyLightLevel(SunSpiritEntity entity, BlockPos pos) {
        return 15;
    }
}
