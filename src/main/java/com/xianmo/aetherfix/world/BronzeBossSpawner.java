package com.xianmo.aetherfix.world;

import com.xianmo.aetherfix.AetherDungeonFix;
import com.xianmo.aetherfix.entity.ModEntityTypes;
import com.xianmo.aetherfix.entity.SliderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 青铜地牢 boss 生成器。
 *
 * 青铜结构走原版 JigsawPlacement（PoolElementStructurePiece），
 * 无法像 SilverBossRoom 那样覆写 piece 的 postProcess，
 * 因此改为：结构生成阶段登记 boss 房中心 -> 服务器 tick 时在已加载区块生成。
 *
 * 保证：
 *  1) 只生成在 boss 房中心（登记时的坐标）；
 *  2) 每个 boss 房都刷新一个 boss（已取消「半径内已有则跳过」的限制）；
 *  3) 持久化（setPersistenceRequired），不会自然消失。
 */
@Mod.EventBusSubscriber(modid = AetherDungeonFix.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
// 【已完成·勿改】本类已实机验证通过，逻辑请勿改动；如需调整请先确认影响面。
public class BronzeBossSpawner {

    /** 待生成 boss 的 boss 房中心坐标（结构生成时登记）。 */
    private static final Set<BlockPos> PENDING = ConcurrentHashMap.newKeySet();

    /** 由 BronzeDungeonStructure 在结构生成阶段调用。 */
    public static void queueBossRoom(BlockPos center) {
        PENDING.add(center.immutable());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        for (ServerLevel level : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            Iterator<BlockPos> it = PENDING.iterator();
            while (it.hasNext()) {
                BlockPos pos = it.next();
                // 区块未加载则等待下次 tick
                if (!level.isLoaded(pos)) {
                    continue;
                }
                //【取消刷新限制】不再做「半径内已有滑行者则跳过」的全局去重，
                // 每个登记过的 boss 房都各自刷新一个 boss。
                SliderEntity slider = ModEntityTypes.SLIDER.get().create(level);
                if (slider == null) {
                    continue;
                }
                double x = pos.getX() + 0.5D;
                double y = (double) floorScanY(level, pos);
                double z = pos.getZ() + 0.5D;
                slider.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
                slider.setPersistenceRequired();
                level.addFreshEntity(slider);
                it.remove();
            }
        }
    }

    /**
     * 从登记点向下扫描，找到地面，返回地面上第一格（可站立）的 Y。
     * 避免直接使用 bounding box 中心导致的悬空（房间有高度，中心偏高）。
     */
    private static int floorScanY(ServerLevel level, BlockPos pos) {
        int minY = level.getMinBuildHeight();
        int y = pos.getY();
        net.minecraft.core.BlockPos.MutableBlockPos mp = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int i = 0; i < 32 && y > minY; i++) {
            if (!level.getBlockState(mp.set(pos.getX(), y - 1, pos.getZ())).isAir()) {
                break;
            }
            y--;
        }
        return y;
    }
}