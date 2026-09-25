package com.xianmo.aetherfix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.xianmo.aetherfix.AetherDungeonFix;
import com.xianmo.aetherfix.entity.projectile.IceCrystal;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * 冰晶渲染器（B5 补充修正版）。
 *
 * 1.18.2 的 EntityRenderer.render() 是【抽象方法】，必须实现，否则编译失败。
 * 本版采用「告示牌（billboard）」方式：把贴图作为始终面向摄像机的方片绘制。
 * 虽不如 1.19.2 的 3D 晶体模型精细，但能正常显示、且绝不崩溃。
 *
 * 贴图来源：1.19.2 textures/entity/projectile/crystals/ice_ball.png
 */
public class IceCrystalRenderer extends EntityRenderer<IceCrystal> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/item/snowball.png");
    /** 显示尺寸（方片半宽）。 */
    private static final float SIZE = 0.25F;

    public IceCrystalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(IceCrystal entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // 面向摄像机
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        float s = SIZE;
        vertex(vc, pose, packedLight, -s, -s, 0.0F, 0.0F);
        vertex(vc, pose, packedLight,  s, -s, 1.0F, 0.0F);
        vertex(vc, pose, packedLight,  s,  s, 1.0F, 1.0F);
        vertex(vc, pose, packedLight, -s,  s, 0.0F, 1.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, int light,
                               float x, float y, float u, float v) {
        vc.vertex(pose.pose(), x, y, 0.0F)
          .color(255, 255, 255, 255)
          .uv(u, v)
          .overlayCoords(OverlayTexture.NO_OVERLAY)
          .uv2(light)
          .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
          .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(IceCrystal entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(IceCrystal entity, BlockPos pos) {
        return 15;
    }
}
