package com.xianmo.aetherfix.client;

import com.xianmo.aetherfix.AetherDungeonFix;
import com.xianmo.aetherfix.entity.SliderEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * S4 Slider 渲染器：使用真正的 SliderModel，四档贴图 + 发光层。
 * 贴图命名空间为本模组（assets/aether_dungeon_fix），不依赖天境本体路径。
 */
// 【已完成·勿改】本类已实机验证通过，逻辑请勿改动；如需调整请先确认影响面。
public class SliderRenderer extends MobRenderer<SliderEntity, SliderModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(AetherDungeonFix.MODID, "slider"), "main");

    private static final ResourceLocation SLIDER_ASLEEP = tex("slider_asleep.png");
    private static final ResourceLocation SLIDER_ASLEEP_CRITICAL = tex("slider_asleep_critical.png");
    private static final ResourceLocation SLIDER_AWAKE = tex("slider_awake.png");
    private static final ResourceLocation SLIDER_AWAKE_CRITICAL = tex("slider_awake_critical.png");

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(AetherDungeonFix.MODID, "textures/entity/mobs/slider/" + name);
    }

    public SliderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SliderModel(ctx.bakeLayer(LAYER)), 2.0F);
        this.addLayer(new SliderGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(SliderEntity entity) {
        if (entity.isAwake()) {
            return entity.isCritical() ? SLIDER_AWAKE_CRITICAL : SLIDER_AWAKE;
        }
        return entity.isCritical() ? SLIDER_ASLEEP_CRITICAL : SLIDER_ASLEEP;
    }
}