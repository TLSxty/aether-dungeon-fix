package com.xianmo.aetherfix.world.piece;

import com.xianmo.aetherfix.AetherDungeonFix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public class GoldIsland extends GoldDungeonPiece {

    public GoldIsland(StructureManager manager, String name, BlockPos pos, Rotation rotation) {
        super(AetherDungeonFix.GOLD_ISLAND_PIECE.get(), manager, name,
              new StructurePlaceSettings().setRotation(rotation), pos);
        this.setOrientation(rotation.rotate(Direction.SOUTH));
    }

    public GoldIsland(StructurePieceSerializationContext context, CompoundTag tag) {
        super(AetherDungeonFix.GOLD_ISLAND_PIECE.get(), tag, context.structureManager(),
              location -> settingsFromTag(tag));
    }
}