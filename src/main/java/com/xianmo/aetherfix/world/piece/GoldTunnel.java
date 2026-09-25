package com.xianmo.aetherfix.world.piece;

import com.xianmo.aetherfix.AetherDungeonFix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/** 黄金地牢 隧道：纯贴模板 tunnel.nbt，无逻辑。 */
public class GoldTunnel extends GoldDungeonPiece {

    public GoldTunnel(StructureManager manager, String name, BlockPos pos, Rotation rotation) {
        super(AetherDungeonFix.GOLD_TUNNEL_PIECE.get(), manager, name,
              new StructurePlaceSettings().setRotation(rotation), pos);
        this.setOrientation(rotation.rotate(Direction.SOUTH));
    }

    public GoldTunnel(StructurePieceSerializationContext context, CompoundTag tag) {
        super(AetherDungeonFix.GOLD_TUNNEL_PIECE.get(), tag, context.structureManager(),
              location -> settingsFromTag(tag));
    }
}
