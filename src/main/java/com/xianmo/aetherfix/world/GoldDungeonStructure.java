package com.xianmo.aetherfix.world;

import com.xianmo.aetherfix.world.piece.GoldIsland;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

import java.util.Optional;
import java.util.Random;

/**
 * 黄金地牢结构生成入口（方案C）。
 *
 * 【2026-09-24 定稿】结构已实机验证通过，调试代码已清理。
 * boss 房（含东侧通道）整体烘焙进 island.nbt，运行时只放置 island 一个片段。
 * 结构与 boss 实体完全解耦——boss（Sun Spirit）为独立实现，不在此类内。
 */
public class GoldDungeonStructure extends StructureFeature<NoneFeatureConfiguration> {

    public GoldDungeonStructure() {
        super(NoneFeatureConfiguration.CODEC, GoldDungeonStructure::createGenerator);
    }

    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

    private static Optional<PieceGenerator<NoneFeatureConfiguration>> createGenerator(
            PieceGeneratorSupplier.Context<NoneFeatureConfiguration> context) {
        if (!context.validBiomeOnTop(Heightmap.Types.WORLD_SURFACE_WG)) {
            return Optional.empty();
        }
        return Optional.of(GoldDungeonStructure::generatePieces);
    }

    private static void generatePieces(StructurePiecesBuilder builder,
                                       PieceGenerator.Context<NoneFeatureConfiguration> context) {
        ChunkPos chunkPos = context.chunkPos();
        StructureManager manager = context.structureManager();
        Random random = context.random();

        int surfaceY = context.chunkGenerator().getBaseHeight(
                chunkPos.getMinBlockX(), chunkPos.getMinBlockZ(),
                Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor());

        int belowTerrain = 2;
        int minY = 35;
        int rangeY = 70;
        int baseHeight = surfaceY - belowTerrain;
        int height = Math.max(baseHeight, minY + random.nextInt(rangeY));

        BlockPos elevatedPos = new BlockPos(chunkPos.getMinBlockX(), height, chunkPos.getMinBlockZ());

        // 大岛（boss 房 + 东侧通道均已烘焙进 island.nbt）
        GoldIsland island = new GoldIsland(manager, "island", elevatedPos, Rotation.NONE);
        builder.addPiece(island);
    }
}
