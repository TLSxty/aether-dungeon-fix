package com.xianmo.aetherfix.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import com.xianmo.aetherfix.ModSounds;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import com.xianmo.aetherfix.entity.projectile.IceCrystal;
import com.xianmo.aetherfix.entity.ai.goal.SunSpiritShootGoal;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Difficulty;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.core.particles.ParticleTypes;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.control.MoveControl;
import com.xianmo.aetherfix.entity.ai.goal.SunSpiritFlyAroundGoal;

/**
 * 烈阳巨灵 Sun Spirit —— 黄金地牢 Boss。
 *
 * 【B2 Boss 条与战斗状态】参照工程内 SliderEntity 模式：
 *   extends PathfinderMob implements Enemy, AetherBossMob
 *   用 ServerBossEvent 实现 Boss 条，setVisible 控制显示。
 *
 * 1.18.2 本体无 SunSpirit 实体类（仅有 Model + 贴图），故从零自建。
 * 属性取值来自 1.19.2 反编译：MAX_HEALTH=500, MOVEMENT_SPEED=0.35。
 *
 * 尚未实现（后续步骤）：
 *   B3 战斗逻辑（isInvulnerableTo / hurt / 冻结机制）；
 *   B4 攻击系统（冰晶 B4-1a + 火球 + 召唤 FireMinion）；
 *   B5 渲染；B6 掉落与钥匙；B7 Boss 房生成接入。
 */
public class SunSpiritEntity extends PathfinderMob implements Enemy, AetherBossMob {

    /** 冻结状态（B3/B4 冰晶机制）。 */
    private static final EntityDataAccessor<Boolean> DATA_FROZEN =
            SynchedEntityData.defineId(SunSpiritEntity.class, EntityDataSerializers.BOOLEAN);
    /** 冻结剩余时长（tick）。 */
    private static final EntityDataAccessor<Integer> DATA_FROZEN_DURATION =
            SynchedEntityData.defineId(SunSpiritEntity.class, EntityDataSerializers.INT);
    /** Boss 名（AetherBossMob 接口要求）。 */
    private static final EntityDataAccessor<Component> DATA_BOSS_NAME =
            SynchedEntityData.defineId(SunSpiritEntity.class, EntityDataSerializers.COMPONENT);

    /** Boss 条（红色，进度样式）。 */
    private final ServerBossEvent bossFight = new ServerBossEvent(
            new TextComponent("Sun Spirit"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);

    /** 台词冷却（B3 使用）。 */
    private int chatCooldown;
    /** 对话进度 0..9，第 10 次右键开战。 */
    private int chatLine;

    /** 飞行环绕中心（问题1修复）。 */
    private Vec3 origin;
    /** 环绕半径 X / Z（默认 9）。 */
    private int xMax = 9;
    private int zMax = 9;
    /** 速度系数：冻结时降为 0.3。 */
    protected double speedModifier = 1.0D;

    public SunSpiritEntity(EntityType<? extends SunSpiritEntity> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.setBossName(new TextComponent("Sun Spirit"));
        // 初始不显示 Boss 条，进入战斗后才显示
        this.bossFight.setVisible(false);
        // 问题1修复：飞行 boss 改用空中移动控制，并记录环绕中心
        this.setNoGravity(true);
        this.moveControl = new BlankMoveControl(this);
        // origin 延迟赋值：构造器执行时 setPos 尚未调用，此处取到的是 (0,0,0)。见 getOrigin() 惰性初始化。
    }

    @Override
    protected void registerGoals() {
        // 问题1修复：环绕飞行（战斗时才启用）
        this.goalSelector.addGoal(0, new SunSpiritFlyAroundGoal(this));
        // B4-2：冰晶发射
        this.goalSelector.addGoal(1, new SunSpiritShootGoal(this));
        // 索敌
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FROZEN, false);
        this.entityData.define(DATA_FROZEN_DURATION, 0);
        this.entityData.define(DATA_BOSS_NAME, new TextComponent("Sun Spirit"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Frozen", this.isFrozen());
        tag.putInt("FrozenDuration", this.getFrozenDuration());
        tag.putBoolean("BossFight", this.isBossFight());
        tag.putInt("ChatCooldown", this.chatCooldown);
        tag.putInt("ChatLine", this.chatLine);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setFrozen(tag.getBoolean("Frozen"));
        this.setFrozenDuration(tag.getInt("FrozenDuration"));
        this.setBossFight(tag.getBoolean("BossFight"));
        this.chatCooldown = tag.getInt("ChatCooldown");
        this.chatLine = tag.getInt("ChatLine");
        // 区块重载后同步 Boss 条名字
        if (this.hasCustomName()) {
            this.bossFight.setName(this.getCustomName());
        }
    }

    // ==================== 同步字段访问器 ====================

    public boolean isFrozen() {
        return this.entityData.get(DATA_FROZEN);
    }

    public void setFrozen(boolean frozen) {
        this.entityData.set(DATA_FROZEN, frozen);
    }

    public int getFrozenDuration() {
        return this.entityData.get(DATA_FROZEN_DURATION);
    }

    public void setFrozenDuration(int duration) {
        this.entityData.set(DATA_FROZEN_DURATION, duration);
    }

    public int getChatCooldown() {
        return this.chatCooldown;
    }

    public void setChatCooldown(int chatCooldown) {
        this.chatCooldown = chatCooldown;
    }

    // ==================== B2 Boss 条与战斗状态 ====================

    /** 是否处于 Boss 战（Boss 条是否可见）。 */
    public boolean isBossFight() {
        return this.bossFight.isVisible();
    }

    /** 开关 Boss 战（同时控制 Boss 条显隐）。 */
    public void setBossFight(boolean active) {
        this.bossFight.setVisible(active);
        if (active && !this.level.isClientSide) {
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.MUSIC_BOSS_SUN_SPIRIT.get(), SoundSource.MUSIC, 16.0F, 1.0F);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level.isClientSide) {
            this.bossFight.setProgress(this.getHealth() / this.getMaxHealth());
        }
        if (this.chatCooldown > 0) {
            this.chatCooldown--;
        }
        // 问题5修复：Boss 战中始终面朝最近的玩家（直接同步朝向，不占 Goal Flag）
        if (this.isBossFight()) {
            Player nearest = this.level.getNearestPlayer(this, 64.0D);
            if (nearest != null) {
                double ddx = nearest.getX() - this.getX();
                double ddz = nearest.getZ() - this.getZ();
                float yaw = (float) (net.minecraft.util.Mth.atan2(ddz, ddx) * 180.0D / Math.PI) - 90.0F;
                this.setYRot(yaw);
                this.yHeadRot = yaw;
                this.yBodyRot = yaw;
            }
        }
        // 问题1修复：冻结时移动速度降为 0.3
        this.speedModifier = this.isFrozen() ? 0.3D : 1.0D;
        // 问题3修复：贴脸灼烧（对齐 1.19.2 tick -> burnEntities）
        if (!this.level.isClientSide && this.getHealth() > 0.0F && !this.isFrozen()) {
            double px = this.getX() + (this.random.nextFloat() - 0.5F) * this.random.nextFloat();
            double py = this.getBoundingBox().minY + this.random.nextFloat() - 0.5D;
            double pz = this.getZ() + (this.random.nextFloat() - 0.5F) * this.random.nextFloat();
            this.level.addParticle(ParticleTypes.FLAME, px, py, pz, 0.0D, -0.075D, 0.0D);
            this.burnEntities();
        }
        // 冻结倒计时（B3）
        if (this.getFrozenDuration() > 0) {
            this.setFrozenDuration(this.getFrozenDuration() - 1);
        } else if (this.isFrozen()) {
            this.setFrozen(false);
        }
        // 冰晶贴脸检测（B3）
        if (!this.level.isClientSide) {
            this.checkIceCrystals();
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossFight.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossFight.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.bossFight.setProgress(0.0F);
        if (!this.level.isClientSide) {
            ClientboundStopSoundPacket pkt = new ClientboundStopSoundPacket(
                    ModSounds.MUSIC_BOSS_SUN_SPIRIT.get().getLocation(), SoundSource.MUSIC);
            for (net.minecraft.server.level.ServerPlayer sp :
                    ((net.minecraft.server.level.ServerLevel) this.level).players()) {
                sp.connection.send(pkt);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    // ==================== B3 战斗逻辑 ====================

    /** 冻结时长常量（1.19.2 原值 175 tick = 8.75 秒）。 */
    public static final int FROZEN_DURATION_TICKS = 175;

    /**
     * 免疫规则（对齐 1.19.2）。
     * - 未冻结：几乎免疫一切，仅【创造模式玩家】与【冰晶】伤害可穿透。
     * - 已冻结：免疫非生物来源；生物（玩家/怪）可正常造成伤害。
     */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (this.isRemoved()) {
            return true;
        }
        // 【后门】/kill 与 bypassInvul 伤害源一律放行（便于调试/清理）
        if (source.isBypassInvul() || source == DamageSource.OUT_OF_WORLD) {
            return false;
        }
        // 【后门】/kill 与 bypassInvul 伤害源一律放行（便于调试/清理）
        if (source.isBypassInvul() || source == DamageSource.OUT_OF_WORLD) {
            return false;
        }
        // 冻结期间：允许生物攻击（形成输出窗口）
        if (this.isFrozen()) {
            Entity src = source.getEntity();
            return !(src instanceof LivingEntity) || src instanceof SunSpiritEntity;
        }
        // 未冻结：仅创造模式玩家 与 冰晶 可穿透
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player
                && player.getAbilities().instabuild) {
            return false;
        }
        return !(source.getDirectEntity() instanceof IceCrystal);
    }

    /**
     * 受伤处理（对齐 1.19.2）。
     * 若伤害来源是冰晶 → 冻结 175 tick + 召唤 FireMinion 并令其锁定冰晶投掷者。
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean flag = super.hurt(source, amount);
        if (!this.level.isClientSide && flag && this.getHealth() > 0.0F) {
            Entity direct = source.getDirectEntity();
            Entity attacker = source.getEntity();
            if (direct instanceof IceCrystal && attacker instanceof LivingEntity livingAttacker) {
                this.setFrozen(true);
                this.setFrozenDuration(FROZEN_DURATION_TICKS);
                

                this.spawnFireMinion(livingAttacker);
            }
        }
        return flag;
    }

    /** 召唤一只 FireMinion（1.18.2 本体已有实体），令其锁定指定目标。 */
    private void spawnFireMinion(LivingEntity target) {
        EntityType<?> type = ForgeRegistries.ENTITIES.getValue(
                new ResourceLocation("aether", "fire_minion"));
        if (type == null) {
            return;
        }
        

        Entity minion = type.create(this.level);
        if (minion == null) {
            return;
        }
        minion.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
        if (minion instanceof Mob mob) {
            mob.setTarget(target);
        }
        this.level.addFreshEntity(minion);
    }

    /** 检测贴身的冰晶，令其对自身造成伤害（进而触发冻结）。 */
    private void checkIceCrystals() {
        for (IceCrystal crystal : this.level.getEntitiesOfClass(
                IceCrystal.class, this.getBoundingBox().inflate(0.1))) {
            

            crystal.doDamage(this);
        }
    }

    // ==================== 右键对话 → 开战（B7 前置） ====================

    /**
     * 右键对话。连续右键 10 次后进入 Boss 战（对齐 1.19.2 m_6071_）。
     * 第 10 次：说最后一句 + 回满血 + setBossFight(true)。
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level.isClientSide && !this.isBossFight() && this.getChatCooldown() <= 0) {
            this.setChatCooldown(14);
            if (this.level.getDifficulty() != Difficulty.PEACEFUL) {
                if (this.chatLine < 9) {
                    this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.0F, 1.0F);
                }
                switch (this.chatLine++) {
                    case 0: this.chatWithNearby(new TextComponent("Who dares intrude upon my golden halls?")); break;
                    case 1: this.chatWithNearby(new TextComponent("You seek the treasure of the sun?")); break;
                    case 2: this.chatWithNearby(new TextComponent("Many have come. None have returned.")); break;
                    case 3: this.chatWithNearby(new TextComponent("The flames of this dungeon are eternal.")); break;
                    case 4: this.chatWithNearby(new TextComponent("Your courage amuses me, mortal.")); break;
                    case 5: this.chatWithNearby(new TextComponent("Do you truly believe you can slay me?")); break;
                    case 6: this.chatWithNearby(new TextComponent("I have burned a thousand heroes to ash.")); break;
                    case 7: this.chatWithNearby(new TextComponent("Turn back, while you still draw breath.")); break;
                    case 8: this.chatWithNearby(new TextComponent("So be it. You have chosen your fate.")); break;
                    case 9:
                        this.chatWithNearby(new TextComponent("Then BURN!"));
                        this.setHealth(this.getMaxHealth());
                        

                        this.setBossFight(true);
                        break;
                    default: break;
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** 向附近 16 格内的玩家发送台词。 */
    private void chatWithNearby(Component message) {
        this.level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0))
                .forEach(p -> p.displayClientMessage(message, false));
    }

    // ==================== AetherBossMob ====================

    // ==================== 问题1修复：移动支撑 ====================

    public Vec3 getOrigin() {
        if (this.origin == null) {
            this.origin = this.position();
        }
        return this.origin;
    }

    public int getXMax() {
        return this.xMax;
    }

    public int getZMax() {
        return this.zMax;
    }

    public double getSpeedModifier() {
        return this.speedModifier;
    }

    // ==================== 问题3修复：近战灼烧 ====================

    /** 灼烧贴脸实体：10 点伤害 + 点燃 8 秒（对齐 1.19.2 burnEntities）。 */
    public void burnEntities() {
        List<Entity> entities = this.level.getEntities(this,
                this.getBoundingBox().move(0.0D, -2.0D, 0.0D).inflate(0.75D, 0.0D, 0.75D));
        for (Entity target : entities) {
            if (!(target instanceof LivingEntity living)) {
                continue;
            }
            living.hurt(this.incinerationSource(), 10.0F);
            living.setSecondsOnFire(8);
        }
    }

    /** 自建灼烧伤害源（1.18.2 无 AetherDamageTypes.incineration）。 */
    private DamageSource incinerationSource() {
        return new EntityDamageSource("aether_incineration", this).setIsFire();
    }

    /** 空移动控制：对齐 1.19.2 BlankMoveControl，不做任何寻路。 */
    private static class BlankMoveControl extends MoveControl {
        public BlankMoveControl(Mob mob) {
            super(mob);
        }

        @Override
        public void tick() {
            // 空实现：飞行由 SunSpiritFlyAroundGoal 直接驱动
        }
    }

    @Override
    public Component getBossName() {
        return this.entityData.get(DATA_BOSS_NAME);
    }

    @Override
    public void setBossName(Component name) {
        this.entityData.set(DATA_BOSS_NAME, name);
        this.bossFight.setName(name);
    }
}
