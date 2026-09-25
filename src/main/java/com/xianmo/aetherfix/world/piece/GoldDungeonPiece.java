package com.xianmo.aetherfix.world.piece;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;
import java.util.function.Function;

/**
 * 黄金地牢所有结构片段的抽象基类（方案C）。
 * 参照工程内 SilverDungeonPiece，沿用其 rotation 修复。
 * 不可 import com.gildedgames.aether.*，方块一律走 ForgeRegistries 字符串查。
 */
public abstract class GoldDungeonPiece extends TemplateStructurePiece {

    /** 1.18.2 的 TemplateStructurePiece 不存 rotation，只能自建此键（照白银根因4）。 */
    public static final String NBT_ROT = "GDRot";

    protected static final RuleProcessor LOCKED_HELLFIRE_STONE;
    protected static final RuleProcessor MOSSY_HOLYSTONE;

    static {
        Block lockedHellfire = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("aether", "locked_hellfire_stone"));
        Block lockedLightHellfire = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("aether", "locked_light_hellfire_stone"));
        Block holystone = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("aether", "holystone"));
        Block mossyHolystone = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("aether", "mossy_holystone"));

        LOCKED_HELLFIRE_STONE = new RuleProcessor(ImmutableList.of(
                new ProcessorRule(new RandomBlockMatchTest(lockedHellfire, 0.1F),
                        AlwaysTrueTest.INSTANCE, lockedLightHellfire.defaultBlockState())));
        MOSSY_HOLYSTONE = new RuleProcessor(ImmutableList.of(
                new ProcessorRule(new RandomBlockMatchTest(holystone, 0.1F),
                        AlwaysTrueTest.INSTANCE, mossyHolystone.defaultBlockState())));
    }

    public GoldDungeonPiece(StructurePieceType type, StructureManager manager, String name,
                            StructurePlaceSettings settings, BlockPos pos) {
        super(type, 0, manager, makeLocation(name), name, settings, pos);
        this.setOrientation(this.getRotation().rotate(Direction.SOUTH));
    }

    public GoldDungeonPiece(StructurePieceType type, CompoundTag tag, StructureManager manager,
                            Function<ResourceLocation, StructurePlaceSettings> settingsFactory) {
        super(type, tag, manager, settingsFactory);
        this.setOrientation(this.getRotation().rotate(Direction.SOUTH));
    }

    /** 模板路径统一为 aether:gold_dungeon/名称 */
    public static ResourceLocation makeLocation(String name) {
        return new ResourceLocation("aether", "gold_dungeon/" + name);
    }

    @Override
    protected ResourceLocation makeTemplateLocation() {
        return makeLocation(this.templateName);
    }

    /** 从 tag 恢复 rotation，供反序列化构造器使用（缺一不可，否则区块重载缺块）。 */
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

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
        try {
            tag.putString(NBT_ROT, this.placeSettings.getRotation().name());
        } catch (Throwable ignored) {
        }
    }

    /** 子类按需覆盖（GoldBossRoom 处理 "Treasure Chest"）。 */
    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level,
                                    Random random, BoundingBox box) {
    }
}
