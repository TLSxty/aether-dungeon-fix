package com.xianmo.aetherfix.chat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 女武神对话限流共享辅助类（非 Mixin 包）。
 *
 * <p>女武神/女武神王的对话全部汇聚到 {@code AbstractValkyrie.chatItUp(Player, Component)}
 * （ValkyrieQueen 则覆写 {@code chatItUp}）。该入口在战斗中被反复调用
 * （尤其每次受击 {@code hurt} 都会触发），导致聊天刷屏。这里以实体的 level.gameTime 为时钟，
 * 对同一实体做固定间隔（60 tick = 3 秒，与 Aether 自身的 chatTimer 一致）限流：
 * 冷却内的对话请求返回 false 并被 Mixin 取消，冷却结束后放行。</p>
 *
 * <p>放在 mixin 包之外，以满足 Mixin 铁律：辅助/鸭子类不得位于 mixins.json 声明的包前缀内。</p>
 */
public final class ValkyrieChatCooldown {

    /** 冷却 tick 数，与 Aether 原版 mobInteract 的 chatTimer(60) 一致 = 3 秒。 */
    public static final int COOLDOWN_TICKS = 60;

    private static final ConcurrentHashMap<UUID, Long> LAST_CHAT_TICK = new ConcurrentHashMap<>();

    private ValkyrieChatCooldown() {
    }

    /**
     * 尝试为指定实体登记一次对话：冷却已过则返回 true 并刷新时间戳，冷却中返回 false。
     *
     * @param entity 发起对话的实体（女武神/女武神王）；null 时直接放行以防意外.
     */
    public static boolean tryMark(LivingEntity entity) {
        if (entity == null) {
            return true;
        }
        UUID uuid = entity.getUUID();
        long now = entity.level.getGameTime();
        Long last = LAST_CHAT_TICK.get(uuid);
        if (last != null && (now - last) < COOLDOWN_TICKS) {
            return false;
        }
        LAST_CHAT_TICK.put(uuid, now);
        return true;
    }

    /* 仅供测试/调试，不参与生产逻辑，防止未使用字段告警。 */
    private static void touch(Entity e) {
        if (e == null) {
            LAST_CHAT_TICK.clear();
        }
    }
}