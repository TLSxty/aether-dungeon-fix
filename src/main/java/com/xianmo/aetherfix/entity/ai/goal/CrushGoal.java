package com.xianmo.aetherfix.entity.ai.goal;

import com.xianmo.aetherfix.entity.SliderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;

/**
 * S3b：滑块碾压拆方块。逻辑照搬 1.19.2 CrushGoal（javap 字节码）。
 * 差异：1.19.2 用 AetherTags.Blocks.SLIDER_UNBREAKABLE；1.18.2 无该类，
 * 改用本工程自建标签 aether:slider_unbreakable。音效用原版 SoundEvents。
 */
// 【已完成·勿改】本类已实机验证通过，逻辑请勿改动；如需调整请先确认影响面。
public class CrushGoal extends Goal {

    /** 自建免拆标签（1.18.2 无 AetherTags.Blocks.SLIDER_UNBREAKABLE）。 */
    private static final TagKey<Block> SLIDER_UNBREAKABLE =
            TagKey.create(net.minecraft.core.Registry.BLOCK_REGISTRY,
                    new ResourceLocation("aether", "slider_unbreakable"));

    private final SliderEntity slider;

    public CrushGoal(SliderEntity slider) {
        this.slider = slider;
        // 原版不设 flags
    }

    @Override
    public boolean canUse() {
        return this.slider.isAwake() && !this.slider.isNoAi()
                && (this.slider.horizontalCollision || this.slider.verticalCollision || this.blocksBetween());
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void tick() {
        Level level = this.slider.level;
        if (!ForgeEventFactory.getMobGriefingEvent(level, this.slider)) {
            return;
        }
        net.minecraft.world.phys.AABB box = this.slider.getBoundingBox().inflate(0.2D);
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX);
        int maxY = (int) Math.floor(box.maxY);
        int maxZ = (int) Math.floor(box.maxZ);
        boolean broke = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!this.isBreakable(state, level, pos)) {
                        continue;
                    }
                    level.destroyBlock(pos, false, this.slider);
                    broke = true;
                }
            }
        }
        if (broke) {
            this.slider.setMoveDelay(this.slider.calculateMoveDelay());
            this.slider.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
    }

    /** 非空气 && !免拆标签 && 硬度>=0 && 硬度<100。 */
    private boolean isBreakable(BlockState state, Level level, BlockPos pos) {
        if (state.isAir()) {
            return false;
        }
        if (state.is(SLIDER_UNBREAKABLE)) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0.0F && hardness < 100.0F;
    }

    /** 与 1.19.2 对齐：扫描前方一格是否有可拆方块。 */
    private boolean blocksBetween() {
        net.minecraft.world.entity.LivingEntity target = this.slider.getTarget();
        if (target == null) {
            return false;
        }
        Level level = this.slider.level;
        for (BlockPos pos : BlockPos.betweenClosed(target.blockPosition(), this.slider.blockPosition())) {
            if (this.isBreakable(level.getBlockState(pos), level, pos)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }
}
