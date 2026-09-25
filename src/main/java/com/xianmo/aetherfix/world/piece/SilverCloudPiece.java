package com.xianmo.aetherfix.world.piece;

import com.xianmo.aetherfix.AetherDungeonFix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;

/**
 * Cloud bed under the silver dungeon. Replaces Aether 1.19.2 buildCloudBed
 * (which used LargeAercloudChunk) with a self-contained piece.
 */
public class SilverCloudPiece extends StructurePiece {

    private final BlockPos origin;
    private final Direction direction;
    private final long seed;

    public SilverCloudPiece(BlockPos origin, Direction direction, long seed) {
        super(AetherDungeonFix.SILVER_CLOUD_PIECE.get(), 0, box(origin));
        this.origin = origin;
        this.direction = direction;
        this.seed = seed;
    }

    public SilverCloudPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(AetherDungeonFix.SILVER_CLOUD_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getInt("ox"), tag.getInt("oy"), tag.getInt("oz"));
        this.direction = Direction.from3DDataValue(tag.getInt("dir"));
        this.seed = tag.getLong("seed");
    }

    private static BoundingBox box(BlockPos o) {
        return new BoundingBox(o.getX() - 80, o.getY() - 6, o.getZ() - 80,
                               o.getX() + 80, o.getY() + 6, o.getZ() + 80);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        BoundingBox bb = this.boundingBox;
        tag.putIntArray("BB", new int[]{bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ()});
        tag.putInt("ox", origin.getX());
        tag.putInt("oy", origin.getY());
        tag.putInt("oz", origin.getZ());
        tag.putInt("dir", direction.get3DDataValue());
        tag.putLong("seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureFeatureManager mgr, ChunkGenerator gen,
                            Random random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        Block cb = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("aether", "cold_aercloud"));
        if (cb == null) cb = Blocks.WHITE_WOOL;
        BlockState cloud = cb.defaultBlockState();

        Random r = new Random(seed);
        int xb, zb, dx, dz;
        switch (direction) {
            case NORTH: xb = 50; zb = 77; dx = -10; dz = -11; break;
            case SOUTH: xb = 50; zb = 77; dx = -40; dz = -66; break;
            case WEST:  xb = 77; zb = 50; dx = -11; dz = -40; break;
            case EAST:  xb = 77; zb = 50; dx = -66; dz = -10; break;
            default:    xb = 50; zb = 77; dx = -10; dz = -11; break;
        }
        BlockPos base = origin.offset(dx, -1, dz);
        int placed = 0;
        for (int t = 0; t < 60; t++) {
            int x = base.getX() + r.nextInt(xb);
            int y = base.getY();
            int z = base.getZ() + r.nextInt(zb);
            int xt = r.nextInt(3) - 1;
            int zt = r.nextInt(3) - 1;
            for (int n = 0; n < 8; n++) {
                x += r.nextInt(3) - 1 + xt;
                if (r.nextBoolean()) y += r.nextInt(3) - 1;
                z += r.nextInt(3) - 1 + zt;
                for (int x1 = x; x1 < x + r.nextInt(4) + 3; x1++) {
                    for (int y1 = y; y1 < y + r.nextInt(2) + 2; y1++) {
                        for (int z1 = z; z1 < z + r.nextInt(4) + 3; z1++) {
                            if (Math.abs(x1 - x) + Math.abs(y1 - y) + Math.abs(z1 - z) >= 4 + r.nextInt(2)) continue;
                            BlockPos bp = new BlockPos(x1, y1, z1);
                            if (!box.isInside(bp)) continue;
                            if (level.isEmptyBlock(bp)) {
                                level.setBlock(bp, cloud, 2);
                                placed++;
                            }
                        }
                    }
                }
            }
        }
    }
}