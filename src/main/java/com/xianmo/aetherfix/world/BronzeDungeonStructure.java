package com.xianmo.aetherfix.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;

import java.util.Optional;

/**
 * 青铜地牢结构入口。【已完成·勿改】
 *
 * 1.18.2 没有通用的 minecraft:jigsaw 结构类型，必须像白银一样自建 StructureFeature，
 * 内部调用 JigsawPlacement.addPieces 完成拼装。
 *
 * 模板与 template_pool 全部复用 1.18.2 本体已有的资产（不打进本模组 jar）：
 *   data/aether/structures/bronze_dungeon/*.nbt
 *   data/aether/worldgen/template_pool/bronze_dungeon/*.json
 * 本类只负责“把它接进世界生成”，并在生成后登记 boss 房坐标。
 *
 * 【勿改·关键】本类产出的是原版 PoolElementStructurePiece，无法覆写 postProcess，
 * 故 Boss 生成采用「登记 + ServerTick」两段式（见 BronzeBossSpawner）。
 */
public class BronzeDungeonStructure extends StructureFeature<JigsawConfiguration> {

    /** 【VOID-FIX】连续多少格空气视为「已越出空岛底部」（空岛体内空心极罕见）。 */
    private static final int VOID_GAP = 8;
    /** 【VOID-FIX】起点相对岛顶的深度：把起点放在地表下方 DUNGEON_DEPTH 格处，便于地牢生长。 */
    private static final int DUNGEON_DEPTH = 16;
    /** 【VOID-FIX】要求空岛体至少比地牢底多出 MIN_ISLAND_MARGIN 格，不够厚的薄岛直接跳过。 */
    private static final int MIN_ISLAND_MARGIN = 8;

    public BronzeDungeonStructure() {
        super(JigsawConfiguration.CODEC, BronzeDungeonStructure::createGenerator);
    }

    /** 【勿改】必须覆写，否则父类返回 null 引发 NPE（白银同款坑）。 */
    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

    /**
     * 【勿改】生成入口。1.18.2 不会自动按 biomes 标签过滤，必须手动调用 validBiomeOnTop。
     */
    private static Optional<PieceGenerator<JigsawConfiguration>> createGenerator(
            PieceGeneratorSupplier.Context<JigsawConfiguration> context) {
        if (!context.validBiomeOnTop(Heightmap.Types.WORLD_SURFACE_WG)) {
            return Optional.empty();
        }
        ChunkPos chunkPos = context.chunkPos();
        int surfaceY = context.chunkGenerator().getBaseHeight(
                chunkPos.getMinBlockX(), chunkPos.getMinBlockZ(),
                Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor());

        // 【勿改·VOID-FIX】虚空保护：天境是空岛维度，虚空区域若生成地牢会悬空。
        if (surfaceY <= context.heightAccessor().getMinBuildHeight()) {
            return Optional.empty();
        }
        if (!hasSolidColumn(context, chunkPos)) {
            return Optional.empty();
        }

        // 【VOID-FIX·抬进空岛】测量结构中心列的空岛厚度，薄岛/空心直接跳过。
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();
        int islandFloorY = scanIslandFloor(context, centerX, centerZ, surfaceY);
        if (islandFloorY == Integer.MIN_VALUE) {
            return Optional.empty();
        }
        int islandThickness = surfaceY - islandFloorY;
        if (islandThickness < DUNGEON_DEPTH + MIN_ISLAND_MARGIN) {
            // 空岛太薄，装不下地牢 -> 跳过，不生成悬空地牢。
            return Optional.empty();
        }

        // 起点放在地表下方 DUNGEON_DEPTH 格处，地牢主体在岛体内生长。
        int startY = surfaceY - DUNGEON_DEPTH;
        BlockPos startPos = new BlockPos(chunkPos.getMinBlockX(), startY, chunkPos.getMinBlockZ());

        Optional<PieceGenerator<JigsawConfiguration>> base =
                JigsawPlacement.addPieces(context, PoolElementStructurePiece::new, startPos, false, false);

        // 【勿改】包裹原生成器：生成完成后遍历片段，登记每个 boss 房的中心坐标。
        // 交给 BronzeBossSpawner 在服务器 tick 时（区块已加载）生成滑行者。
        return base.map(gen -> (PieceGenerator<JigsawConfiguration>) (builder, ctx) -> {
            gen.generatePieces(builder, ctx);
            try {
                for (StructurePiece p : builder.build().pieces()) {
                    if (String.valueOf(p).contains("boss_room")) {
                        BoundingBox bb = p.getBoundingBox();
                        BronzeBossSpawner.queueBossRoom(bb.getCenter());
                    }
                }
            } catch (Throwable ignored) {
                // 登记失败不影响结构本身生成
            }
        });
    }

    /**
     * 【VOID-FIX·空岛底界】在指定列从表面向下扫描，返回空岛底部最后一块实心方块的 Y
     * （也是常规生成的下边界）。一旦遇到连续 >=VOID_GAP 格的空气，即认定已穿过岛底落到虚空，
     * 返回 MIN_VALUE 让调用方放弃生成。
     */
    private static int scanIslandFloor(PieceGeneratorSupplier.Context<JigsawConfiguration> context,
                                       int x, int z, int surfaceY) {
        LevelHeightAccessor ha = context.heightAccessor();
        int minY = ha.getMinBuildHeight();
        NoiseColumn col = context.chunkGenerator().getBaseColumn(x, z, ha);
        int lastSolid = Integer.MIN_VALUE;
        int airRun = 0;
        for (int y = surfaceY; y > minY; y--) {
            if (!col.getBlock(y).isAir()) {
                lastSolid = y;
                airRun = 0;
            } else {
                airRun++;
                // 空岛体内空心极罕见；连续 VOID_GAP 格空气 => 岛底之下的虚空。
                if (airRun >= VOID_GAP) {
                    return lastSolid;
                }
            }
        }
        return lastSolid;
    }

    /**
     * 【勿改·VOID-FIX】检查该 chunk 的四角列是否存在实体方块。
     * 天境为漂浮空岛，虚空区域四角全为空气，此时应放弃生成。
     */
    private static boolean hasSolidColumn(PieceGeneratorSupplier.Context<JigsawConfiguration> context,
                                          ChunkPos chunkPos) {
        LevelHeightAccessor ha = context.heightAccessor();
        int minY = ha.getMinBuildHeight();
        int maxY = ha.getMaxBuildHeight();
        int[][] corners = new int[][]{
                {chunkPos.getMinBlockX(), chunkPos.getMinBlockZ()},
                {chunkPos.getMaxBlockX(), chunkPos.getMinBlockZ()},
                {chunkPos.getMinBlockX(), chunkPos.getMaxBlockZ()},
                {chunkPos.getMaxBlockX(), chunkPos.getMaxBlockZ()}
        };
        int solidCorners = 0;
        for (int[] c : corners) {
            NoiseColumn col = context.chunkGenerator().getBaseColumn(c[0], c[1], ha);
            for (int y = minY; y < maxY; y++) {
                if (!col.getBlock(y).isAir()) {
                    solidCorners++;
                    break;
                }
            }
        }
        // 至少有一角落在实地上才允许生成
        return solidCorners > 0;
    }
}