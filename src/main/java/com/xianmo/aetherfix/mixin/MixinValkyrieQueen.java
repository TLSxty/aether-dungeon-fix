package com.xianmo.aetherfix.mixin;

import com.xianmo.aetherfix.chat.ValkyrieChatCooldown;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为女武神王 {@code ValkyrieQueen.chatItUp} 增加对话限流。
 *
 * <p>ValkyrieQueen 覆写了 AbstractValkyrie.chatItUp（前置黄色 [bossName]: 再 sendMessage），
 * 且其一段对白流程（进入对话、应战、平静、拒绝、击败、告别、玩家死亡）都会经由该入口，
 * chatWithNearby 还会遍历区域内玩家逐个调用 chatItUp——若每拍都触发则会刷屏。
 * 注入 HEAD + cancellable，冷却中直接取消。</p>
 */
@Mixin(targets = "com.gildedgames.aether.common.entity.monster.dungeon.ValkyrieQueen")
public abstract class MixinValkyrieQueen {

    @Inject(method = "chatItUp", at = @At("HEAD"), cancellable = true, remap = false)
    private void aetherfix$throttleChat(Player player, Component component, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!ValkyrieChatCooldown.tryMark(self)) {
            ci.cancel();
        }
    }
}