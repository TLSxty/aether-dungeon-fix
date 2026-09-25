package com.xianmo.aetherfix.entity.ai.goal;

import com.xianmo.aetherfix.entity.SliderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 1.19.2 SetPathUpOrDownGoal 的 1.18.2 移植。 */
// 【已完成·勿改】本类已实机验证通过，逻辑请勿改动；如需调整请先确认影响面。
public class SetPathUpOrDownGoal extends Goal {

    private final SliderEntity slider;

    public SetPathUpOrDownGoal(SliderEntity slider) {
        this.slider = slider;

    }

    @Override
    public boolean canUse() {
        if (this.slider.getMoveDelay() != 1 || this.slider.getTargetPoint() != null) {
            return false;
        }
        if (this.slider.getRandom().nextInt(3) != 0) {
            return false;
        }
        Direction direction = this.slider.getMoveDirection();
        return direction == null || direction.getAxis() != Direction.Axis.Y;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void tick() {
        Vec3 targetPos = getTargetOrCurrentPosition(this.slider);
        if (targetPos == null) {
            return;
        }
        Vec3 selfPos = this.slider.position();
        AABB pathBox = this.slider.getBoundingBox().inflate(targetPos.x - selfPos.x, 0.0D, targetPos.z - selfPos.z);
        Direction dirY = selfPos.y > targetPos.y ? Direction.DOWN : Direction.UP;
        pathBox = SliderEntity.calculateAdjacentBox(pathBox, dirY);
        pathBox = pathBox.move(0.0D, targetPos.y - selfPos.y, 0.0D);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int minX = (int) Math.floor(pathBox.minX);
        int minZ = (int) Math.floor(pathBox.minZ);
        for (int x = minX; (double) x < pathBox.maxX; x++) {
            for (int z = minZ; (double) z < pathBox.maxZ; z++) {
                mutable.set(x, targetPos.y, z);
                if (this.slider.level.getBlockState(mutable).is(SliderEntity.SLIDER_UNBREAKABLE)) {
                    return;
                }
            }
        }
        double newY = dirY == Direction.UP ? Math.max(targetPos.y, selfPos.y + 1.0D) : targetPos.y;
        this.slider.setMoveDirection(dirY);
        this.slider.setTargetPoint(new Vec3(selfPos.x, newY, selfPos.z));
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    private static Vec3 getTargetOrCurrentPosition(SliderEntity slider) {
        LivingEntity target = slider.getTarget();
        if (target == null) {
            return null;
        }
        return target.position();
    }
}
