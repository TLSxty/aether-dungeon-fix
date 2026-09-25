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

        // 【勿改】1.19.2 的 aboveBottom=32 语义 -> 从地表往下 32 格作为起点
        BlockPos startPos = new BlockPos(chunkPos.getMinBlockX(), surfaceY - 32, chunkPos.getMinBlockZ());

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