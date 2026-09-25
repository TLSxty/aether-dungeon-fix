package com.xianmo.aetherfix.world.piece;

import com.xianmo.aetherfix.AetherDungeonFix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.Random;

public class SilverDungeonRoom extends SilverDungeonPiece {

    private static final ResourceLocation LOOT =
            new ResourceLocation("aether", "chests/dungeon/silver/silver_dungeon");

    public SilverDungeonRoom(StructureManager manager, String name, BlockPos pos, Rotation rotation) {
        super(AetherDungeonFix.SILVER_ROOM_PIECE.get(), manager, name,
              makeSettings(manager, name, rotation), pos);
        this.setOrientation(rotation.rotate(Direction.SOUTH));
    }

    public SilverDungeonRoom(StructurePieceSerializationContext context, CompoundTag tag) {
        super(AetherDungeonFix.SILVER_ROOM_PIECE.get(), tag, context.structureManager(),
              location -> settingsFromTag(tag));
    }

    /**
     * 2026-09-21 修复（缺旋转轴心）：
     * 1.19.2 原版 SilverDungeonRoom.makeSettings 会按模板尺寸计算旋转轴心：
     *     pivot = (sizeX / 2 - 4, 0, sizeZ / 2 - 4)
     * 并调用 setRotationPivot(pivot) 后再 setRotation。
     * 缺这一步时模板绕 (0,0,0) 旋转，rotation=90/180/270 会整体漂到相邻象限，
     * 表现为「同一存档里有的朝向正常、有的朝向全乱」。
     * 此处仅使用 1.18.2 API（StructureManager#get / StructureTemplate#getSize）。
     */
    private static StructurePlaceSettings makeSettings(StructureManager manager, String name, Rotation rotation) {
        StructurePlaceSettings settings = new StructurePlaceSettings();
        BlockPos pivot = makePivot(manager, name);
        if (pivot != null) {
            settings.setRotationPivot(pivot);
        }
        settings.setRotation(rotation);
        return settings;
    }

    /**
     * **不要修改** 必须返回 null（不使用自定义旋转轴心）。
     * 1.19.2 原版 pivot=(sizeX/2-4, 0, sizeZ/2-4) 只适用约 32 格大模板；
     * 本套最小模板 chest_room 2x2x2 / staircase 4x9x4 代入得负值 -3 / -2，
     * 会把房间整体甩出格子 6~7 格。临界条件 sizeX > 8 轴心才为正。
     */
    private static BlockPos makePivot(StructureManager manager, String name) {
        try {
            Optional<StructureTemplate> optional = manager.get(makeLocation(name));
            if (optional.isPresent()) {
                Vec3i size = optional.get().getSize();
                return null; // 2026-09-21: pivot=(size/2-4) 对本套小模板为负，会把房间推出格子 6~7 格
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level,
                                    Random random, BoundingBox box) {
        if (marker.equals("Chest")) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            if (random.nextInt(5) > 1) {
                level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
                RandomizableContainerBlockEntity.setLootTable(level, random, pos, LOOT);
            } else {
                Block mimic = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("aether", "chest_mimic"));
                if (mimic != null) {
                    level.setBlock(pos, mimic.defaultBlockState(), 2);
                } else {
                    level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
                    RandomizableContainerBlockEntity.setLootTable(level, random, pos, LOOT);
                }
            }
        }
    }
}