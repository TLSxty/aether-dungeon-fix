package com.xianmo.aetherfix.event;

import com.xianmo.aetherfix.AetherDungeonFix;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 三地牢统一钥匙开奖（方案 B 扩展版）。
 * 右键任意处触发（RightClickItem）；战利品直接进背包，溢出落地；消耗钥匙（创造不消耗）。
 * 三把钥匙各自指向各自的奖励表。
 *
 * 注意：不可 import com.gildedgames.aether.*（jar 不在编译路径），
 * 故钥匙判定采用 ForgeRegistries 取注册名的字符串比对。
 */
@Mod.EventBusSubscriber(modid = AetherDungeonFix.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KeyRewardHandler {

    /** 钥匙注册名 -> 对应奖励表。 */
    private static final Map<ResourceLocation, ResourceLocation> KEY_TO_REWARD = new HashMap<>();

    static {
        KEY_TO_REWARD.put(new ResourceLocation("aether", "bronze_dungeon_key"),
                new ResourceLocation("aether", "chests/dungeon/bronze/bronze_dungeon_reward"));
        KEY_TO_REWARD.put(new ResourceLocation("aether", "gold_dungeon_key"),
                new ResourceLocation("aether", "chests/dungeon/gold/gold_dungeon_reward"));
        KEY_TO_REWARD.put(new ResourceLocation("aether", "silver_dungeon_key"),
                new ResourceLocation("aether", "chests/dungeon/silver/silver_dungeon_reward"));
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack held = event.getItemStack();
        if (held.isEmpty()) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(held.getItem());
        if (id == null) {
            return;
        }
        ResourceLocation reward = KEY_TO_REWARD.get(id);
        if (reward == null) {
            return;
        }
        // 只在服务端处理，避免两端重复发奖
        if (!(event.getWorld() instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getWorld();
        Player player = event.getPlayer();
        event.setCanceled(true);

        LootTable table = level.getServer().getLootTables().get(reward);
        if (table == LootTable.EMPTY) {
            return;
        }
        LootContext ctx = new LootContext.Builder(level)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .withRandom(level.getRandom())
                .create(LootContextParamSets.CHEST);

        List<ItemStack> drops = table.getRandomItems(ctx);
        int given = 0;
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!player.getInventory().add(stack)) {
                if (!stack.isEmpty()) {
                    player.drop(stack, false);
                }
            }
            given++;
        }

        // 只有确实发出奖励才提示 + 播放音效 + 消耗钥匙
        if (given > 0) {
            player.displayClientMessage(new TextComponent("§a宝藏从钥匙中解封！"), false);
            player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }
    }
}
