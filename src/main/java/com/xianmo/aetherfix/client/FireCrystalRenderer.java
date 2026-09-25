package com.xianmo.aetherfix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.xianmo.aetherfix.entity.projectile.FireCrystal;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** 火晶渲染器（问题4）—— 贴图用原版烈焰弹 fire_charge。 */
public class FireCrystalRenderer extends EntityRenderer<FireCrystal> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/item/fire_charge.png");
    private static final float SIZE = 0.3F;

    public FireCrystalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(FireCrystal entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
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
    public ResourceLocation getTextureLocation(FireCrystal entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(FireCrystal entity, BlockPos pos) {
        return 15;
    }
}
