package com.xianmo.aetherfix.world;

import com.xianmo.aetherfix.world.piece.SilverBossRoom;
import com.xianmo.aetherfix.world.piece.SilverCloudPiece;
import com.xianmo.aetherfix.world.piece.SilverTemplePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 白银地牢结构生成入口。
 *
 * 1.18.2 用的是旧结构系统 StructureFeature + PieceGeneratorSupplier。
 * 生成流程：createGenerator（含生物群系过滤）-> generatePieces（拼装片段）。
 *
 * 【不要修改】以下三处均经过游戏内六个实例验证：
 *   1. step() 必须返回 SURFACE_STRUCTURES，否则父类返回 null 会导致 NPE。
 *   2. createGenerator 内必须手动调用 validBiomeOnTop，
 *      1.18.2 不会自动按 biomes 标签过滤，不调用会全世界生成。
 *   3. generatePieces 内必须保留 random.nextInt(4)，
 *      它消耗一次随机数以保持后续随机序列与官方一致，只是返回值被忽略。
 */
public class SilverDungeonStructure extends StructureFeature<NoneFeatureConfiguration> {

    public SilverDungeonStructure() {
        super(NoneFeatureConfiguration.CODEC, SilverDungeonStructure::createGenerator);
    }

    /** 【不要修改】必须覆写，否则父类返回 null 引发 NPE */
    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

    /**
     * 【不要修改】必须手动做生物群系过滤。
     * 1.18.2 的 isFeatureChunk 只判断本方法是否返回 present，
     * biomes 标签只是塞进 Context 的参数，不会自动生效。
     */
    private static Optional<PieceGenerator<NoneFeatureConfiguration>> createGenerator(
            PieceGeneratorSupplier.Context<NoneFeatureConfiguration> context) {
        boolean ok = context.validBiomeOnTop(Heightmap.Types.WORLD_SURFACE_WG);
        if (!ok) {
            return Optional.empty();
        }
        return Optional.of(SilverDungeonStructure::generatePieces);
    }

    private static void generatePieces(StructurePiecesBuilder builder,
                                       PieceGenerator.Context<NoneFeatureConfiguration> context) {
        ChunkPos chunkPos = context.chunkPos();
        StructureManager manager = context.structureManager();
        Random random = context.random();

        // 【不要修改】锁定生成方向。
        // 实验证明 rot=NONE 生成完美结构，rot 非 NONE 时内容物会因旋转移出结构外。
        // 这里仍消耗一次 nextInt(4) 以保持后续随机序列与官方一致，只是忽略其值。
        random.nextInt(4);
        Rotation rotation = Rotation.NONE;
        Direction direction = rotation.rotate(Direction.SOUTH);

        int surfaceY = context.chunkGenerator().getBaseHeight(
                chunkPos.getMinBlockX(), chunkPos.getMinBlockZ(),
                Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor());

        // 【不要修改】高度随机化参数，maxY=104 为实测值（非 1.19.x 的 128）
        int belowTerrain = 2;
        int aboveTerrain = 18;
        int maxY = 104;
        int minY = 35;
        int rangeY = 70;
        int baseHeight = surfaceY - belowTerrain;
        int height;
        if (random.nextInt(5) < 3) {
            height = baseHeight + aboveTerrain;
            if (height < maxY) {
                height += random.nextInt(maxY - height);
            }
        } else {
            height = Math.max(baseHeight, minY + random.nextInt(rangeY));
        }

        // 【不要修改】结构起始偏移：北 54 格 + 西 15 格
        BlockPos input = new BlockPos(chunkPos.getMinBlockX(), height, chunkPos.getMinBlockZ());
        BlockPos elevatedPos = input
                .offset(rotation.rotate(Direction.NORTH).getNormal().multiply(54))
                .offset(rotation.rotate(Direction.WEST).getNormal().multiply(15));
        builder.addPiece(new SilverCloudPiece(elevatedPos, direction, random.nextLong()));

        SilverTemplePiece rear = new SilverTemplePiece(manager, "rear", elevatedPos, rotation);
        builder.addPiece(rear);

        // 【不要修改】Boss 房偏移（+3 y，水平 5 格）
        BlockPos bossPos = elevatedPos.offset(
                (direction.getStepX() + direction.getStepZ()) * 5, 3,
                (direction.getStepZ() - direction.getStepX()) * 5);
        SilverBossRoom bossRoom = new SilverBossRoom(manager, "boss_room", bossPos, rotation);
        builder.addPiece(bossRoom);

        // 【不要修改】骨架起点 = elevatedPos 加 rear 的跨度，务必放在 boss 房之后计算
        int xo = direction.getStepX() * rear.getBoundingBox().getXSpan();
        int zo = direction.getStepZ() * rear.getBoundingBox().getZSpan();
        BlockPos offsetPos = elevatedPos.offset(xo, 0, zo);
        bossRoom.setGridStart(offsetPos);
        SilverTemplePiece skelPiece = new SilverTemplePiece(manager, "skeleton", offsetPos, rotation);
        builder.addPiece(skelPiece);
        try {
            List<BlockPos> spawnPoints =
                    new SilverDungeonBuilder(random, 3, 3, 3)
                        .assembleDungeon(builder, manager,
                        offsetPos,
                        rotation, direction);
            // 把可刷怪房间坐标注入 boss 房，供守卫女武神分散刷新使用
            bossRoom.setSpawnPoints(spawnPoints);
        } catch (Throwable t) {
            // 与原版一致的静默处理；如需排查可临时打印 t
        }
    }
}