package com.xianmo.aetherfix.entity.ai.goal;

import com.xianmo.aetherfix.entity.SunSpiritEntity;
import com.xianmo.aetherfix.entity.projectile.FireCrystal;
import com.xianmo.aetherfix.entity.projectile.IceCrystal;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** 烈阳巨灵·发射晶球（2026-09-25 重做）。
 * 修复：原实现发射在 start() 且 canContinueToUse() 恒 true，Goal 启动后永不停下，
 * canUse() 不再被调用，导致只发射一次。现改用内部计时器在 tick() 中持续发射。 */
public class SunSpiritShootGoal extends Goal {

    private static final int INTERVAL = 40;
    private static final int ICE_EVERY = 5;

    private final SunSpiritEntity sunSpirit;
    private int shootInterval = INTERVAL;
    private int crystalCount = ICE_EVERY;

    public SunSpiritShootGoal(SunSpiritEntity sunSpirit) {
        this.sunSpirit = sunSpirit;
        // 不占 MOVE，避免被常驻的 FlyAroundGoal 永久独占
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.sunSpirit.isBossFight();
    }

    @Override
    public boolean canContinueToUse() {
        return this.sunSpirit.isBossFight();
    }

    @Override
    public void start() {
        this.shootInterval = INTERVAL;
    }

    @Override
    public void tick() {
        if (--this.shootInterval > 0) {
            return;
        }
        this.shootInterval = INTERVAL;
        if (--this.crystalCount <= 0) {
            this.crystalCount = ICE_EVERY;
            this.sunSpirit.level.addFreshEntity(IceCrystal.create(this.sunSpirit.level, this.sunSpirit));
            this.sunSpirit.level.playSound(null, this.sunSpirit.getX(), this.sunSpirit.getY(), this.sunSpirit.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 3.0F, 1.0F);
        } else {
            this.sunSpirit.level.addFreshEntity(FireCrystal.create(this.sunSpirit.level, this.sunSpirit));
            this.sunSpirit.level.playSound(null, this.sunSpirit.getX(), this.sunSpirit.getY(), this.sunSpirit.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 1.0F);
        }
    }
}
