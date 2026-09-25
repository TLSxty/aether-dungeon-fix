package com.xianmo.aetherfix.entity.projectile;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * 水晶投掷物抽象基类。
 *
 * 对齐 1.19.2 com.aetherteam.aether.entity.projectile.crystal.AbstractCrystal
 * （extends Projectile）。
 *
 * 2026-09-25 修复：补上命中检测。
 * 原实现只做位移 + 寿命，从不调用 onHitEntity/onHitBlock，
 * 导致子类的命中逻辑（冰晶撞墙反弹、命中消失）全是死代码。
 * 现按 1.19.2 的 tick() 补上 ProjectileUtil 命中检测。
 */
public abstract class AbstractCrystal extends Projectile {

    /** 空中存活 tick 数。 */
    protected int ticksInAir;

    protected AbstractCrystal(EntityType<? extends AbstractCrystal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        // 无额外同步字段
    }

    @Override
    public void tick() {
        super.tick();
        this.ticksInAir++;

        // 命中检测（服务端）：命中后分派给子类
        if (!this.level.isClientSide) {
            HitResult hit = ProjectileUtil.getHitResult(this,
                    e -> e.isPickable() && !e.isSpectator());
            if (hit.getType() == HitResult.Type.ENTITY) {
                this.onHitEntity((EntityHitResult) hit);
            } else if (hit.getType() == HitResult.Type.BLOCK) {
                this.onHitBlock((BlockHitResult) hit);
            }
            if (this.isRemoved()) {
                return;
            }
        }

        this.tickMovement();

        if (!this.level.isClientSide && this.ticksInAir > this.getLifeSpan()) {
            this.onExpire();
            this.spawnExplosionParticles();
            this.discard();
        }
    }

    /** 寿命到期时的回调（子类可覆写，如火焰晶引爆）。 */
    protected void onExpire() {
    }

    /** 每 tick 的飞行位移。 */
    protected void tickMovement() {
        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }

    /** 生成爆炸粒子。 */
    public void spawnExplosionParticles() {
        ParticleOptions particle = this.getExplosionParticle();
        if (particle == null) {
            return;
        }
        for (int i = 0; i < 8; i++) {
            this.level.addParticle(particle,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.5,
                    (this.random.nextDouble() - 0.5) * 0.5,
                    (this.random.nextDouble() - 0.5) * 0.5);
        }
    }

    /** 爆炸粒子类型（子类实现）。 */
    protected abstract ParticleOptions getExplosionParticle();

    /** 命中音效。 */
    protected SoundEvent getImpactExplosionSoundEvent() {
        return SoundEvents.GLASS_BREAK;
    }

    /** 寿命（tick）。 */
    public int getLifeSpan() {
        return 200;
    }

    /**
     * 可被选中：对齐 1.19.2（返回 true）。
     * 原返回 false 会导致玩家准星无法锁定冰晶，
     * 「用剑把冰晶打回去」的核心机制无法成立。
     */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TicksInAir", this.ticksInAir);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ticksInAir = tag.getInt("TicksInAir");
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
