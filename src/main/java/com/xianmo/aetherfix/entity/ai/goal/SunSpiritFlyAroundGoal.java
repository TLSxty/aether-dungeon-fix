package com.xianmo.aetherfix.entity.ai.goal;

import com.xianmo.aetherfix.entity.SunSpiritEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 烈阳巨灵·环绕飞行。
 * 2026-09-25 定稿：
 *  1 撞墙反射：按速度主轴镜像 rotation（原版逻辑）
 *  2 NUDGE 卡死保底：连续 1.5 秒几乎不动时，朝结构中心拨动并叠加 ±45~90 度随机偏移
 * 已移除：预测式转向（与反弹互搏）、硬性钳制、位置回退。
 */
public class SunSpiritFlyAroundGoal extends Goal {

    private final SunSpiritEntity sunSpirit;
    private float rotation;
    private int courseChangeTimer;
    private int bounceCool;
    private double lastX = Double.NaN;
    private double lastZ = Double.NaN;
    private int stuckTimer;
    private int graceTicks;

    public SunSpiritFlyAroundGoal(SunSpiritEntity sunSpirit) {
        this.sunSpirit = sunSpirit;
        this.rotation = sunSpirit.getRandom().nextFloat() * 360.0F;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() { return this.sunSpirit.isBossFight(); }

    @Override
    public boolean canContinueToUse() { return true; }

    @Override
    public void tick() {
        double speed = this.sunSpirit.getAttributeValue(Attributes.MOVEMENT_SPEED)
                * this.sunSpirit.getSpeedModifier();
        double cx = this.sunSpirit.getX();
        double cz = this.sunSpirit.getZ();

        // 1. 撞墙反射（原版逻辑）
        if (this.bounceCool > 0) { this.bounceCool--; }
        if (this.sunSpirit.horizontalCollision && this.bounceCool <= 0) {
            Vec3 v = this.sunSpirit.getDeltaMovement();
            if (Math.abs(v.x) >= Math.abs(v.z)) {
                this.rotation = 180.0F - this.rotation;
            } else {
                this.rotation = -this.rotation;
            }
            this.rotation += (this.sunSpirit.getRandom().nextFloat() - 0.5F) * 30.0F;
            this.bounceCool = 8;
            
        }

        // 2. NUDGE 卡死保底
        if (this.graceTicks > 0) {
            this.graceTicks--;
            this.stuckTimer = 0;
        } else if (!Double.isNaN(this.lastX)
                && Math.abs(cx - this.lastX) + Math.abs(cz - this.lastZ) < 0.03D) {
            if (++this.stuckTimer > 30) {
                this.stuckTimer = 0;
                this.graceTicks = 15;
                // 朝结构中心拨动，再叠加 ±20~30 度随机偏移（收窄，避免沿墙漂移）
                Vec3 o = this.sunSpirit.getOrigin();
                this.rotation = faceAngle(cx, cz, o.x, o.z);
                float off = (this.sunSpirit.getRandom().nextFloat() - 0.5F) * 60.0F;
                if (Math.abs(off) < 20.0F) {
                    off = off < 0.0F ? -20.0F : 20.0F;
                }
                this.rotation += off;
                
            }
        } else {
            this.stuckTimer = 0;
        }
        this.lastX = cx;
        this.lastZ = cz;

        // 常规随机微调
        if (--this.courseChangeTimer <= 0) {
            this.courseChangeTimer = 20;
            if (this.sunSpirit.getRandom().nextInt(4) == 0) {
                this.rotation += this.sunSpirit.getRandom().nextFloat() * 40.0F - 20.0F;
            }
        }

        this.rotation = Mth.wrapDegrees(this.rotation);
        float rad = this.rotation * ((float) Math.PI / 180F);
        double x = Mth.sin(rad) * speed;
        double z = -Mth.cos(rad) * speed;
        this.sunSpirit.setDeltaMovement(x, 0.0D, z);


    }

    private static float faceAngle(double cx, double cz, double tx, double tz) {
        return (float) Math.toDegrees(Math.atan2(tx - cx, -(tz - cz)));
    }
}
