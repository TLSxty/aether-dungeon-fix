package com.xianmo.aetherfix.entity.ai.goal;

import com.xianmo.aetherfix.entity.SliderEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * S3a 滑块移动 Goal。逻辑照搬 1.19.2 SliderMoveGoal，用 1.18.2 API 实现。
 *
 * 【已完成·勿改】本类为滑块核心移动逻辑，已实机验证通过（能追、能撞、能拆，卡死可自愈）。
 * 与 1.19.2 的差异：1.18.2 无 ENTITY_SLIDER_MOVE 音效，故省略 start() 的音效播放。
 *
 * 关键数值（勿改）：
 *   STALL_TICKS   = 40      停滞检测阈值：位置 40 tick 无变化即判定卡死
 *   STALL_EPS_SQR = 0.0016  停滞判定阈值（平方距离，约 0.04 格）
 * 卡死自愈：stuckDir 记录卡死方向，下次算出的方向若与之相同则换轴（alternateDirection）。
 */
public class SliderMoveGoal extends Goal {

    private final SliderEntity slider;
    private Vec3 targetPoint;
    private float velocity;

    /** 【勿改】停滞检测参数。 */
    private static final int STALL_TICKS = 40;
    private static final double STALL_EPS_SQR = 0.0016;
    private int stallTicks;
    private Vec3 lastStallPos;
    private Direction stuckDir;

    public SliderMoveGoal(SliderEntity slider) {
        this.slider = slider;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /** 仅在 awake、非 noAi、冷却结束时启动；目标点由 SliderEntity.findTargetPoint 计算。 */
    @Override
    public boolean canUse() {
        boolean base = this.slider.isAwake() && !this.slider.isNoAi() && this.slider.getMoveDelay() <= 0;
        if (!base) {
            return false;
        }
        this.targetPoint = this.slider.findTargetPoint();
        return this.targetPoint != null;
    }

    /** 【勿改】任一碰撞（水平/垂直）即不可继续，交由其它 Goal 接管。 */
    @Override
    public boolean canContinueToUse() {
        return this.slider.isAwake()
                && !this.slider.isNoAi()
                && this.slider.getMoveDelay() <= 0
                && !this.slider.horizontalCollision
                && !this.slider.verticalCollision;
    }

    @Override
    public void start() {
        this.slider.setMoveDirection(null);
    }

    @Override
    public void tick() {
        if (this.targetPoint == null) {
            this.stop();
            return;
        }
        if (this.checkStall()) {
            return;
        }
        // 【勿改】撞墙且已达最大速度 -> 停止
        if (this.slider.horizontalCollision && this.velocity >= this.slider.getMaxVelocity() * 0.98F) {
            this.stop();
            return;
        }
        Direction direction = this.getMoveDirection(this.targetPoint);
        double ad = axisDistance(this.targetPoint, this.slider.position(), direction);
        if (ad <= 0.0D) {
            this.stop();
            return;
        }
        // 【勿改】线性加速：每 tick 增加 getVelocityIncrease()，上限 getMaxVelocity()
        if (this.velocity < this.slider.getMaxVelocity()) {
            this.velocity = Math.min(this.slider.getMaxVelocity(),
                    this.velocity + this.slider.getVelocityIncrease());
        }
        this.slider.setDeltaMovement(new Vec3(
                direction.getStepX() * this.velocity,
                direction.getStepY() * this.velocity,
                direction.getStepZ() * this.velocity));
    }

    @Override
    public void stop() {
        this.slider.setMoveDelay(this.slider.calculateMoveDelay());
        this.slider.setTargetPoint(null);
        this.targetPoint = null;
        this.velocity = 0.0F;
        this.slider.setDeltaMovement(Vec3.ZERO);
        this.stallTicks = 0;
        this.lastStallPos = null;
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    /** 【勿改】卡死自愈：连续 STALL_TICKS 无位移 -> 记录 stuckDir 后停止，下次算方向时换轴。 */
    private boolean checkStall() {
        Vec3 now = this.slider.position();
        if (this.lastStallPos == null) {
            this.lastStallPos = now;
            return false;
        }
        if (now.distanceToSqr(this.lastStallPos) < STALL_EPS_SQR) {
            this.stallTicks++;
        } else {
            this.stallTicks = 0;
            this.lastStallPos = now;
        }
        if (this.stallTicks >= STALL_TICKS) {
            this.stuckDir = this.slider.getMoveDirection();
            this.stallTicks = 0;
            this.lastStallPos = null;
            this.stop();
            return true;
        }
        return false;
    }

    /** 【勿改】未设定方向时按目标点计算；若与上次卡死方向相同则换轴。 */
    private Direction getMoveDirection(Vec3 targetPoint) {
        Direction direction = this.slider.getMoveDirection();
        if (direction == null) {
            double dx = targetPoint.x - this.slider.getX();
            double dy = targetPoint.y - this.slider.getY();
            double dz = targetPoint.z - this.slider.getZ();
            direction = SliderEntity.calculateDirection(dx, dy, dz);
            if (direction != null && direction == this.stuckDir) {
                direction = this.alternateDirection(dx, dz);
            }
            this.stuckDir = null;
            this.slider.setMoveDirection(direction);
        }
        return direction;
    }

    /** 【勿改】换轴：优先走位移较大的轴，若该轴方向正是卡死方向则改走另一轴。 */
    private Direction alternateDirection(double dx, double dz) {
        double ax = Math.abs(dx);
        double az = Math.abs(dz);
        if (ax >= az) {
            Direction d = dx > 0.0D ? Direction.EAST : Direction.WEST;
            if (d != this.stuckDir) {
                return d;
            }
            return dz > 0.0D ? Direction.SOUTH : Direction.NORTH;
        }
        Direction d = dz > 0.0D ? Direction.SOUTH : Direction.NORTH;
        if (d != this.stuckDir) {
            return d;
        }
        return dx > 0.0D ? Direction.EAST : Direction.WEST;
    }

    /** 目标点在指定方向上的投影距离（用于判断是否已到达）。 */
    private static double axisDistance(Vec3 a, Vec3 b, Direction direction) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return dx * direction.getStepX() + dy * direction.getStepY() + dz * direction.getStepZ();
    }
}