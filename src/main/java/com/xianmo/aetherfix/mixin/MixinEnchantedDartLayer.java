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
 * 修复 Aether 附魔飞镖渲染层的空指针。
 *
 * <p>原版 {@code EnchantedDartLayer.numStuck} 用 {@code AetherPlayer.get(player).orElse(null)}
 * 取 capability，随后无判空直接调用 {@code getEnchantedDartCount()}。玩家死亡/未附加 capability
 * 时该 LazyOptional 为空（预期行为），于是抛 NPE，并被 ForgeIngameGui 吞掉，
 * 表现为死亡界面异常、实体/粒子渲染失效。</p>
 *
 * <p>此处在方法头部拦截：拿不到 capability 就当作 0 根飞镖。</p>
 *
 * <p>目标为第三方 aether 类且运行期方法名为字面量 → remap=false；
 * {@code numStuck(T)} 泛型擦除为 {@code LivingEntity}，故处理器签名必须写 LivingEntity。</p>
 */
@Mixin(targets = "com.gildedgames.aether.client.renderer.player.layer.EnchantedDartLayer")
public abstract class MixinEnchantedDartLayer {

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
