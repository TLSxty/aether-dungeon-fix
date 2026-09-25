package com.xianmo.aetherfix.world.piece;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import java.util.Random;
import java.util.function.Function;

/**
 * 白银地牢所有结构片段的抽象基类。
 *
 * 【核心修复 2026-09-21】
 * 1.18.2 的 TemplateStructurePiece 的 NBT 不保存 placement rotation。
 * 区块重载或跨区块重建时，反序列化构造器里的 new StructurePlaceSettings()
 * 会让 rotation 默认为 NONE，导致西墙的 sideways（顺时针 90 度）丢失，
 * 墙从沿 z 展开退化为沿 x 展开，表现为「z 段缺墙」。
 *
 * 修复方案：自建 NBT_ROT 键保存 rotation，反序列化时用 settingsFromTag 恢复。
 * 4 个子类的反序列化构造器均须调用 settingsFromTag(tag)，不得改用
 * new StructurePlaceSettings()，否则缺墙问题会复发。
 */
public abstract class SilverDungeonPiece extends TemplateStructurePiece {

    /**
     * 【不要修改】NBT 键名。
     * 1.18.2 的 TemplateStructurePiece 不存 rotation，只能自建此键。
     * 改名会导致已有存档中已生成的结构片段反序列化时丢失 rotation。
     */
    public static final String NBT_ROT = "SDRot";

    public SilverDungeonPiece(StructurePieceType type, StructureManager manager, String name,
                              StructurePlaceSettings settings, BlockPos pos) {
        super(type, 0, manager, makeLocation(name), name, settings, pos);
        this.setOrientation(this.getRotation().rotate(Direction.SOUTH));
    }

    public SilverDungeonPiece(StructurePieceType type, CompoundTag tag, StructureManager manager,
                              Function<ResourceLocation, StructurePlaceSettings> settingsFactory) {
        super(type, tag, manager, settingsFactory);
        this.setOrientation(this.getRotation().rotate(Direction.SOUTH));
    }

    /** 模板路径统一为 aether:silver_dungeon/名称 */
    public static ResourceLocation makeLocation(String name) {
        return new ResourceLocation("aether", "silver_dungeon/" + name);
    }

    @Override
    protected ResourceLocation makeTemplateLocation() {
        return makeLocation(this.templateName);
    }

    /**
     * 【不要修改】从 tag 恢复 rotation 的 settings，供反序列化构造器使用。
     * 这是修复「缺墙」的关键：区块重载时必须从此处恢复 rotation，
     * 否则西墙的顺时针 90 度旋转会丢失。
     */
    public static StructurePlaceSettings settingsFromTag(CompoundTag tag) {
        StructurePlaceSettings s = new StructurePlaceSettings();
        if (tag.contains(NBT_ROT)) {
            try {
                s.setRotation(Rotation.valueOf(tag.getString(NBT_ROT)));
            } catch (Throwable ignored) {
            }
        }
        return s;
    }

    /**
     * 【不要修改】保存时把 rotation 写进 NBT。
     * 与 settingsFromTag 配对使用，缺一不可。
     */
    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
        try {
            tag.putString(NBT_ROT, this.placeSettings.getRotation().name());
        } catch (Throwable ignored) {
        }
    }

    /** 子类按需覆盖以处理 data marker（如 Chest / Treasure Chest） */
    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level,
                                    Random random, BoundingBox box) {
    }
}