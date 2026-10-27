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
 * 为普通女武神 {@code AbstractValkyrie.chatItUp} 增加对话限流。
 *
 * <p>Valkyrie 不覆写 chatItUp，其每次受击 {@code hurt} 都会无条件调用基类 chatItUp，
 * 造成刷屏。注入 HEAD + cancellable，冷却中直接取消。目标类为第三方 aether（属其自身命名空间），
 * 且 {@code chatItUp} 在运行期即为该字面名，故 remap=false（也符合本项目知识库对第三方类注入的经验）。</p>
 */
@Mixin(targets = "com.gildedgames.aether.common.entity.monster.dungeon.AbstractValkyrie")
public abstract class MixinAbstractValkyrie {

    @Inject(method = "chatItUp", at = @At("HEAD"), cancellable = true, remap = false)
    private void aetherfix$throttleChat(Player player, Component component, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!ValkyrieChatCooldown.tryMark(self)) {
            ci.cancel();
        }
    }
}