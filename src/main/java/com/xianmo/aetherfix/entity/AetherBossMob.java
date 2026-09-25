package com.xianmo.aetherfix.entity;

import net.minecraft.network.chat.Component;

/**
 * 轻量 Boss 接口，对齐 1.18.2 天境本体的 com.gildedgames.aether.common.entity.BossMob。
 * 本体该接口也只有这两个方法，此处自建以免引入 aether 依赖。
 */
public interface AetherBossMob {

    Component getBossName();

    void setBossName(Component name);
}
