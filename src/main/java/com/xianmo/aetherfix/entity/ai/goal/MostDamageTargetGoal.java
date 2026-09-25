package com.xianmo.aetherfix.entity.ai.goal;

import com.xianmo.aetherfix.entity.SliderEntity;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * S3c 第 4 项：优先攻击「造成伤害最高」的玩家，而非最近的玩家。
 *
 * 对照源：1.18.2 天境本体（已是 1.18.2 API，逐行移植）
 *   com.gildedgames.aether.common.entity.ai.goal.target.MostDamageTargetGoal
 *   D:/天境移植/提取数据缓存/ref_MostDamageTargetGoal_1182.java
 *
 * 与 1.19.2 无关（用户明确指令：只用 1.18.2 API）。
 *
 * 【移植期修正】原版 tickAggro 内 attackers.forEach(this::decayAggro) 在遍历同一
 * map 时做 put/remove，fastutil 会抛 ConcurrentModificationException；
 * 本实现改为先快照 key 再改，语义不变。
 */
// 【已完成·勿改】本类已实机验证通过，逻辑请勿改动；如需调整请先确认影响面。
public class MostDamageTargetGoal extends TargetGoal {

    /** [还原 <clinit>] 战斗目标判定条件：忽略视线、忽略隐身。 */
    private static final TargetingConditions HURT_BY_TARGETING =
            TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

    private final Object2DoubleMap<LivingEntity> attackers = new Object2DoubleOpenHashMap<>();
    private int lastHurtTimestamp;
    @Nullable
    private LivingEntity primaryTarget;
    private final float calmDownRate;
    private int aiTicks;

    public MostDamageTargetGoal(SliderEntity slider) {
        this(slider, 1.0F);
    }

    public MostDamageTargetGoal(SliderEntity slider, float calmDownRate) {
        super(slider, true);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        this.calmDownRate = calmDownRate;
    }

    @Override
    public boolean canUse() {
        this.tickAggro();
        if (this.attackers.isEmpty()) {
            return false;
        }
        int timestamp = this.mob.getLastHurtByMobTimestamp();
        LivingEntity lastHurtBy = this.mob.getLastHurtByMob();
        if (lastHurtBy != null && timestamp != this.lastHurtTimestamp) {
            this.primaryTarget = this.getStrongestAttacker();
        }
        return this.primaryTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        this.tickAggro();
        return this.mob.getLastHurtByMobTimestamp() == this.lastHurtTimestamp
                && super.canContinueToUse();
    }

    @Override
    public void start() {
        this.mob.setTarget(this.primaryTarget);
        this.lastHurtTimestamp = this.mob.getLastHurtByMobTimestamp();
        super.start();
    }

    /** 由 SliderEntity.hurt 在受击时调用，累加该攻击者的伤害总量。 */
    public void addAggro(LivingEntity attacker, double damage) {
        this.attackers.mergeDouble(attacker, damage, Double::sum);
    }

    private void tickAggro() {
        if (++this.aiTicks >= 10) {
            this.aiTicks = 0;
            List<LivingEntity> snapshot = new ArrayList<>(this.attackers.keySet());
            for (LivingEntity attacker : snapshot) {
                this.decayAggro(attacker, this.attackers.getDouble(attacker));
            }
        }
    }

    /** [还原 lambda$tickAggro$0] 每 10 tick 衰减 calmDownRate，并剔除失效攻击者。 */
    private void decayAggro(LivingEntity attacker, double damage) {
        double newDamage = damage - (double) this.calmDownRate;
        if (attacker.isAlive() && newDamage > 0.0D && this.canAttack(attacker, HURT_BY_TARGETING)) {
            this.attackers.put(attacker, newDamage);
        } else {
            this.attackers.remove(attacker);
        }
    }

    /** 取累计伤害最高的攻击者（迭代器实现，避免不同 fastutil 版本方法引用签名差异）。 */
    @Nullable
    private LivingEntity getStrongestAttacker() {
        LivingEntity best = null;
        double bestDamage = 0.0D;
        for (Object2DoubleMap.Entry<LivingEntity> entry : this.attackers.object2DoubleEntrySet()) {
            if (entry.getDoubleValue() > bestDamage) {
                bestDamage = entry.getDoubleValue();
                best = entry.getKey();
            }
        }
        return best;
    }
}