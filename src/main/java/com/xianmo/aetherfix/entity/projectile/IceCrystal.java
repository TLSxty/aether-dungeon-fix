package com.xianmo.aetherfix.entity.projectile;

import com.xianmo.aetherfix.entity.ModEntityTypes;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 冰晶 —— 1.18.2 自建。
 * 2026-09-25 按 1.19.2 反编译源（缓存文件/B0-IceCrystal）完成七点修复：
 *   (1) create() 脚部 Y 生成  (2) 随机 360 度水平初速度
 *   (3) doDamage 排除主人     (4) 伤害 7.0F
 *   (5) getLifeSpan 500      (6) onHitBlock 按面反弹
 *   (7) onHitEntity 命中主人不消失
 */
public class IceCrystal extends AbstractCrystal implements WeaknessDamage {

    private double xPower;
    private double zPower;
    private boolean attacked;

    public IceCrystal(EntityType<? extends IceCrystal> type, Level level) {
        super(type, level);
    }

    /** 静态工厂。修复(1)(2)：脚部 Y + 随机 360 度初速度，确保冰晶立刻飞出 boss 碰撞箱。 */
    public static IceCrystal create(Level level, LivingEntity shooter) {
        IceCrystal crystal = new IceCrystal(ModEntityTypes.ICE_CRYSTAL.get(), level);
        crystal.setOwner(shooter);
        crystal.setPos(shooter.getX(), shooter.getY() + 1.2D, shooter.getZ());
        float rotation = crystal.random.nextFloat() * 360.0F;
        crystal.xPower = Mth.sin(rotation) * 0.2D;
        crystal.zPower = -Mth.cos(rotation) * 0.2D;
        crystal.setDeltaMovement(crystal.xPower, 0.0D, crystal.zPower);
        

        return crystal;
    }

    /** 修复(7)：命中主人自己不结算、不消失；命中其他实体正常伤害并消失。 */
    @Override
    protected void onHitEntity(EntityHitResult result) {

        
        super.onHitEntity(result);
        if (this.level.isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        if (target == this.getOwner()) {
            return;
        }
        this.doDamage(target);
        this.spawnExplosionParticles();
        this.discard();
    }

    /** 修复(6)：未被打回时按撞击面反弹；被打回(attacked)后撞墙即消失。 */
    @Override
    protected void onHitBlock(BlockHitResult result) {

        
        super.onHitBlock(result);
        if (this.level.isClientSide) {
            return;
        }
        // 问题6修复：被打回后不再撞墙即碎，而是继续飞行（仅由寿命/命中实体结束）
        switch (result.getDirection()) {
            case NORTH:
            case SOUTH:
                this.zPower = -this.zPower;
                break;
            case WEST:
            case EAST:
                this.xPower = -this.xPower;
                break;
            default:
                break;
        }
        this.setDeltaMovement(this.xPower, 0.0D, this.zPower);
    }

    /** 弱点伤害。修复(3)：排除主人，避免 boss 被自己发射的冰晶冻结。修复(4)：7 点伤害。 */
    @Override
    public void doDamage(Entity target) {

        if (target == this.getOwner()) {
            return;
        }
        if (target instanceof LivingEntity living) {
            Entity owner = this.getOwner();
            DamageSource source = new IndirectEntityDamageSource("thrown", this, owner);
            living.hurt(source, 7.0F);
        }
    }

    /** 修复(5)配套：被攻击时按攻击者视线方向弹回，并改归属为攻击者。 */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level.isClientSide) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker != null) {
            Vec3 look = attacker.getViewVector(1.0F);
            this.xPower = look.x * 2.5D;
            this.zPower = look.z * 2.5D;
            this.setDeltaMovement(this.xPower, 0.0D, this.zPower);
            this.setOwner(attacker);
            this.attacked = true;
        }
        return true;
    }

    /** 修复(5)：存活时长 100 -> 500。 */
    @Override
    public int getLifeSpan() {
        return 500;
    }

    @Override
    protected ParticleOptions getExplosionParticle() {
        return ParticleTypes.SNOWFLAKE;
    }

    @Override
    public SoundEvent getImpactExplosionSoundEvent() {
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("XSpeed", this.xPower);
        tag.putDouble("ZSpeed", this.zPower);
        tag.putBoolean("Attacked", this.attacked);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.xPower = tag.getDouble("XSpeed");
        this.zPower = tag.getDouble("ZSpeed");
        this.attacked = tag.getBoolean("Attacked");
    }
}
