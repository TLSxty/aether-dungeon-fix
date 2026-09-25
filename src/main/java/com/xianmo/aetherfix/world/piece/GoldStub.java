package com.xianmo.aetherfix.world.piece;

import com.xianmo.aetherfix.AetherDungeonFix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/** 黄金地牢 小岛桩：纯贴模板 stub.nbt，无逻辑。 */
public class GoldStub extends GoldDungeonPiece {

    public GoldStub(StructureManager manager, String name, BlockPos pos, Rotation rotation) {
        super(AetherDungeonFix.GOLD_STUB_PIECE.get(), manager, name,
              new StructurePlaceSettings().setRotation(rotation), pos);
        this.setOrientation(rotation.rotate(Direction.SOUTH));
    }

    public GoldStub(StructurePieceSerializationContext context, CompoundTag tag) {
        super(AetherDungeonFix.GOLD_STUB_PIECE.get(), tag, context.structureManager(),
              location -> settingsFromTag(tag));
    }
}
