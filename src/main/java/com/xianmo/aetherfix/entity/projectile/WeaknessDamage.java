package com.xianmo.aetherfix.entity.projectile;

import net.minecraft.world.entity.Entity;

/**
 * 「弱点伤害」标记接口。
 *
 * 对齐 1.19.2 com.aetherteam.aether.entity.projectile.crystal.WeaknessDamage。
 * 实现此接口的投掷物，其 doDamage 对特定 Boss 造成的是「弱点伤害」。
 */
public interface WeaknessDamage {

    /** 对目标实体造成弱点伤害。 */
    void doDamage(Entity target);
}
