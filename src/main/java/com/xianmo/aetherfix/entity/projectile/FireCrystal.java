package com.xianmo.aetherfix.entity.projectile;

import com.xianmo.aetherfix.entity.ModEntityTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 火晶（问题4 还原）—— 对齐 1.19.2 com.aetherteam.aether.entity.projectile.crystal.FireCrystal。
 * 贴图走原版烈焰弹（fire_charge），逻辑为自建简化版。
 */
public class FireCrystal extends AbstractCrystal {

    private double xPower;
    private double yPower;
    private double zPower;

    public FireCrystal(EntityType<? extends FireCrystal> type, Level level) {
        super(type, level);
    }

    /** 静态工厂：胸口 +1.0 生成，随机 360 度水平初速 0.5 + 随机垂直分量。 */
    public static FireCrystal create(Level level, LivingEntity shooter) {
        FireCrystal crystal = new FireCrystal(ModEntityTypes.FIRE_CRYSTAL.get(), level);
        crystal.setOwner(shooter);
        crystal.setPos(shooter.getX(), shooter.getY() + 1.0D, shooter.getZ());
        float rotation = crystal.random.nextFloat() * 360.0F;
        crystal.xPower = Mth.sin(rotation) * 0.5D;
        crystal.zPower = -Mth.cos(rotation) * 0.5D;
        crystal.yPower = 0.0D;
        crystal.setDeltaMovement(crystal.xPower, 0.0D, crystal.zPower);
        return crystal;
    }

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
        if (target instanceof LivingEntity living) {
            Entity owner = this.getOwner();
            DamageSource source = new EntityDamageSource("aether_fire_crystal", this).setIsFire();
            if (living.hurt(source, 15.0F)) {
                living.setSecondsOnFire(6);
                this.spawnExplosionParticles();
                this.discard();
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level.isClientSide) {
            return;
        }
        switch (result.getDirection()) {
            case NORTH:
            case SOUTH:
                this.zPower = -this.zPower;
                break;
            case UP:
            case DOWN:
                this.yPower = -this.yPower;
                break;
            case WEST:
            case EAST:
                this.xPower = -this.xPower;
                break;
            default:
                break;
        }
        this.setDeltaMovement(this.xPower, this.yPower, this.zPower);
    }

    /** 到期自爆：点燃周围一小片地板。 */
    @Override
    protected void onExpire() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos bp = this.blockPosition().offset(dx, dy, dz);
                    if (this.level.isEmptyBlock(bp) && !this.level.isEmptyBlock(bp.below())) {
                        this.level.setBlockAndUpdate(bp, Blocks.FIRE.defaultBlockState());
                    }
                }
            }
        }
    }
    @Override
    protected ParticleOptions getExplosionParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    public SoundEvent getImpactExplosionSoundEvent() {
        return SoundEvents.GENERIC_EXTINGUISH_FIRE;
    }

    @Override
    public int getLifeSpan() {
        return 300;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("XSpeed", this.xPower);
        tag.putDouble("YSpeed", this.yPower);
        tag.putDouble("ZSpeed", this.zPower);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.xPower = tag.getDouble("XSpeed");
        this.yPower = tag.getDouble("YSpeed");
        this.zPower = tag.getDouble("ZSpeed");
    }
}
