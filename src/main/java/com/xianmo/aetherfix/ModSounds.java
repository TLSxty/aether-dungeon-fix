package com.xianmo.aetherfix;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Sun Spirit 战斗音乐。1.18.2 本体无 music.boss.*，须自建。 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AetherDungeonFix.MODID);

    public static final RegistryObject<SoundEvent> MUSIC_BOSS_SUN_SPIRIT =
            SOUND_EVENTS.register("music.boss.sun_spirit",
                    () -> new SoundEvent(new ResourceLocation(AetherDungeonFix.MODID, "music.boss.sun_spirit")));
}
