package com.xianmo.aetherfix.entity.ai.goal;

import com.xianmo.aetherfix.entity.SliderEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.List;

// 【已完成·勿改】本类已实机验证通过，逻辑请勿改动；如需调整请先确认影响面。
public class CollideGoal extends Goal {
    private static final DamageSource CRUSH = new DamageSource("aether_crush");
    private static final float CRUSH_DAMAGE = 6.0F;
    private final SliderEntity slider;

    public CollideGoal(SliderEntity slider) {
        this.slider = slider;
        // 原版 CollideGoal 不设 flags；占 MOVE 会挤掉 SliderMoveGoal
    }

    @Override
    public boolean canUse() {
        if (!this.slider.isAwake() || this.slider.isNoAi()) return false;
        return this.slider.attackCooldown() <= 0 || this.slider.getDeltaMovement().length() > 0.08D;
    }

    @Override
    public boolean canContinueToUse() { return this.canUse(); }

    @Override
    public void tick() {
        AABB box = this.slider.getBoundingBox().inflate(0.1D);
        List<Entity> hits = this.slider.level.getEntities(this.slider, box);
        if (hits.isEmpty()) return;
        for (Entity e : hits) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            living.hurt(CRUSH, CRUSH_DAMAGE);
            if (living instanceof Player p2 && p2.getMainHandItem().is(Items.SHIELD) && p2.isBlocking()) {
                p2.getCooldowns().addCooldown(Items.SHIELD, 100);
                p2.stopUsingItem();
            }
            living.setDeltaMovement(living.getDeltaMovement().scale(4.0D).add(0.25D, 0.25D, 0.25D));
        }
        this.slider.setMoveDelay(this.slider.calculateMoveDelay());
        this.slider.setAttackCooldown(20);
        this.slider.setMoveDirection(null);
        SoundEvent se = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("aether", "entity.slider.collide"));
        if (se != null) this.slider.playSound(se, 2.5F, 0.2F + this.slider.getRandom().nextFloat() * 0.7F);
        this.slider.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean isInterruptable() { return true; }
}
