package com.xianmo.aetherfix.world.piece;

import com.xianmo.aetherfix.AetherDungeonFix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public class SilverFloorPiece extends SilverDungeonPiece {

    public SilverFloorPiece(StructureManager manager, String name, BlockPos pos, Rotation rotation) {
        super(AetherDungeonFix.SILVER_FLOOR_PIECE.get(), manager, name,
              new StructurePlaceSettings().setRotation(rotation), pos);
        this.setOrientation(rotation.rotate(Direction.SOUTH));
    }

    public SilverFloorPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(AetherDungeonFix.SILVER_FLOOR_PIECE.get(), tag, context.structureManager(),
              location -> settingsFromTag(tag));
    }
}
