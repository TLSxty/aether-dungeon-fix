package com.xianmo.aetherfix.entity.ai.goal;

import com.xianmo.aetherfix.entity.SliderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 1.19.2 AvoidObstaclesGoal 的 1.18.2 移植：前方若有不可破坏方块，则向上找一层可通行平面。 */
// 【已完成·勿改】本类已实机验证通过，逻辑请勿改动；如需调整请先确认影响面。
public class AvoidObstaclesGoal extends Goal {

    private final SliderEntity slider;

    /** 向上扫描的最大格数，防止整列不可破坏时死循环。 */
    private static final int MAX_SCAN_UP = 16;

    public AvoidObstaclesGoal(SliderEntity slider) {
        this.slider = slider;
    }

    @Override
    public boolean canUse() {
        if (!this.slider.isAwake() || this.slider.isNoAi() || this.slider.getMoveDelay() != 1) {
            return false;
        }
        Direction direction = this.slider.getMoveDirection();
        return direction != null && direction.getAxis() != Direction.Axis.Y;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void tick() {
        Vec3 target = this.slider.findTargetPoint();
        if (target == null) {
            return;
        }
        Direction direction = SliderEntity.calculateDirection(
                target.x - this.slider.getX(),
                target.y - this.slider.getY(),
                target.z - this.slider.getZ());
        AABB box = SliderEntity.calculateAdjacentBox(this.slider.getBoundingBox(), direction);
        BlockPos from = new BlockPos((int) Math.floor(box.minX), (int) Math.floor(box.minY), (int) Math.floor(box.minZ));
        BlockPos to = new BlockPos((int) Math.ceil(box.maxX) - 1, (int) Math.ceil(box.maxY) - 1, (int) Math.ceil(box.maxZ) - 1);
        boolean blocked = false;
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (this.slider.level.getBlockState(pos).is(SliderEntity.SLIDER_UNBREAKABLE)) {
                blocked = true;
                break;
            }
        }
        if (blocked) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            int startY = (int) Math.floor(box.minY);
            int y = startY;
            // 迭代上限：整列不可破坏时防止死循环（原版无上限，1.18.2 移植期加固）
            while (blocked && y < startY + MAX_SCAN_UP) {
                y++;
                blocked = false;
                for (int x = (int) Math.floor(box.minX); (double) x < box.maxX; x++) {
                    for (int z = (int) Math.floor(box.minZ); (double) z < box.maxZ; z++) {
                        if (this.slider.level.getBlockState(mutable.set(x, y, z)).is(SliderEntity.SLIDER_UNBREAKABLE)) {
                            blocked = true;
                        }
                    }
                }
            }
            if (blocked) {
                // 向上 MAX_SCAN_UP 格仍被阻塞：放弃本次绕行，不设目标点，避免卡死
                return;
            }
            Vec3 pos = this.slider.position();
            this.slider.setTargetPoint(new Vec3(pos.x, (double) y, pos.z));
            this.slider.setMoveDirection(Direction.UP);
        }
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }
}
