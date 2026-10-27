package com.xianmo.aetherfix.mixin;

import com.gildedgames.aether.core.capability.player.AetherPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修复 Aether 黄金飞镖渲染层的空指针（与 EnchantedDartLayer 同一缺陷）。
 *
 * <p>原版 {@code GoldenDartLayer.numStuck} 用 {@code orElse(null)} 取 AetherPlayer 后无判空解引用。
 * 玩家身上挂了金飞镖且 capability 为空时会 NPE。此处在方法头部拦截，取不到 capability 就当 0 根。</p>
 */
@Mixin(targets = "com.gildedgames.aether.client.renderer.player.layer.GoldenDartLayer")
public abstract class MixinGoldenDartLayer {

    @Inject(method = "numStuck", at = @At("HEAD"), cancellable = true, remap = false)
    private void aetherfix$numStuckGuard(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (entity instanceof Player player) {
            LazyOptional<AetherPlayer> opt = AetherPlayer.get(player);
            if (!opt.isPresent()) {
                cir.setReturnValue(0);
            }
        }
    }
}
