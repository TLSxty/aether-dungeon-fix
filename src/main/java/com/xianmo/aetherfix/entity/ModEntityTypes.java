package com.xianmo.aetherfix.entity;

import com.xianmo.aetherfix.AetherDungeonFix;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import com.xianmo.aetherfix.entity.projectile.IceCrystal;
import com.xianmo.aetherfix.entity.projectile.FireCrystal;
import net.minecraftforge.registries.RegistryObject;

// Slider 已实机验证通过，其注册勿动；SunSpirit / IceCrystal 为本轮新增。
public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, AetherDungeonFix.MODID);

    public static final RegistryObject<EntityType<SliderEntity>> SLIDER =
            ENTITY_TYPES.register("slider", () -> EntityType.Builder
                    .of(SliderEntity::new, MobCategory.MONSTER)
                    .sized(2.0F, 2.0F)
                    .clientTrackingRange(10)
                    .build(AetherDungeonFix.MODID + ":slider"));

    public static final RegistryObject<EntityType<SunSpiritEntity>> SUN_SPIRIT =
            ENTITY_TYPES.register("sun_spirit", () -> EntityType.Builder
                    .of(SunSpiritEntity::new, MobCategory.MONSTER)
                    .sized(2.5F, 3.4F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(AetherDungeonFix.MODID + ":sun_spirit"));

    /** 冰晶投掷物（B4-1a 自建）—— 生存模式下唯一能伤害 Sun Spirit 的手段。 */
    public static final RegistryObject<EntityType<IceCrystal>> ICE_CRYSTAL =
            ENTITY_TYPES.register("ice_crystal", () -> EntityType.Builder
                    .<IceCrystal>of(IceCrystal::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(AetherDungeonFix.MODID + ":ice_crystal"));

    /** 火晶投掷物（问题4 还原）—— 贴图用原版烈焰弹。 */
    public static final RegistryObject<EntityType<FireCrystal>> FIRE_CRYSTAL =
            ENTITY_TYPES.register("fire_crystal", () -> EntityType.Builder
                    .<FireCrystal>of(FireCrystal::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(AetherDungeonFix.MODID + ":fire_crystal"));

    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(SLIDER.get(), SliderEntity.createAttributes().build());
        event.put(SUN_SPIRIT.get(), SunSpiritEntity.createAttributes().build());
    }
}
