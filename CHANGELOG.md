# 更新日志

## v1.1 — 2026-10-28

本次为累计更新，包含崩溃修复、汉化、地牢生成调整与三处第三方模组缺陷的 Mixin 兜底修复。

### 修复

- **FireCrystal 崩溃修复**：修正 `IndirectEntityDamageSource` 的构造用法，火焰水晶命中不再抛出崩溃。
- **Aether 飞镖渲染层空指针修复**（新增 3 个 Mixin）：
  - Aether 的 `EnchantedDartLayer` / `GoldenDartLayer` / `PoisonDartLayer` 三处 `numStuck` 均使用
    `AetherPlayer.get(player).orElse(null)` 取 capability，随后无判空直接解引用，
    在玩家死亡 / capability 未附加时抛 NPE（被 `ForgeIngameGui` 吞掉），
    表现为死亡界面异常、实体 / 粒子 / 末地传送门渲染失效。
  - 现于三个子类方法头部拦截：取不到 capability 即视为 0 根飞镖。三处一并修复，
    避免只修一处后金飞镖 / 毒飞镖路径再次崩溃。
- **女武神 / 女武神王对话限流**（新增 2 个 Mixin）：
  - 二者的对话均汇聚到 `chatItUp(Player, Component)`，战斗中每次受击都会无条件触发，造成聊天刷屏。
  - 注入 `chatItUp` 头部，按实体 UUID 施加 60 tick（3 秒）冷却，冷却内的重复对话被取消。
  - 冷却时长与 Aether 原版 `mobInteract` 的 `chatTimer` 一致。

### 改动

- **汉化**：补齐 94 条缺失的 `zh_cn` 翻译键；太阳精灵（Sun Spirit）名称改为可翻译组件。
- **青铜地牢**：地牢改为在岛内生成；移除 BOSS 生成的半径去重限制，改为每房间独立生成 BOSS。
- **白银地牢守卫调整**：
  - 守卫女武神数量由 3~5 名（3 + rand(3)）下调为 **1~2 名**（1 + rand(2)）。
  - 出生点由「Boss 房间中心 ±3 格」改为**散布到整个 3×3×3 网格地牢的各可刷怪房间**，
    不再全部堆积在 Boss 房间。女武神王仍生成于 Boss 房间且保持持久。

### 基建

- 引入 Mixin 支持：MixinGradle 0.7.38 + Mixin 0.8.5（annotationProcessor）、
  `aether_dungeon_fix.mixins.json` 配置、jar manifest 写入 `MixinConfigs` 属性。
- 全部 Mixin 目标为第三方 Aether 类，运行期方法名均为字面量，统一使用 `remap = false`，
  refmap 留空（符合预期）。

### 说明

- 已验证：构建通过（`reobfJar`），游戏内无 `Mixin apply failed`、无渲染报错，
  死亡界面与重生后渲染恢复正常。
