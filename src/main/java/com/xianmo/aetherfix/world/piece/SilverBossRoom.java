package com.xianmo.aetherfix.world.piece;

import com.xianmo.aetherfix.AetherDungeonFix;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;

public class SilverBossRoom extends SilverDungeonPiece {

    private boolean populated = false;

    private BlockPos roomOrigin;
    private BlockPos gridStart;

    public void setGridStart(BlockPos p) {
        this.gridStart = p;
    }

    public SilverBossRoom(StructureManager m, String n, BlockPos p, Rotation r) {
        super(AetherDungeonFix.SILVER_BOSS_ROOM.get(), m, n,
              new StructurePlaceSettings().setRotation(r).setIgnoreEntities(true), p);
        this.roomOrigin = p;
    }

    public SilverBossRoom(StructurePieceSerializationContext c, CompoundTag t) {
        super(AetherDungeonFix.SILVER_BOSS_ROOM.get(), t, c.structureManager(),
              loc -> settingsFromTag(t).setIgnoreEntities(true));
        this.populated = t.getBoolean("Populated");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
        tag.putBoolean("Populated", this.populated);
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level,
                                    Random random, BoundingBox box) {
        if (marker.equals("Treasure Chest")) {
            RandomizableContainerBlockEntity.setLootTable(level, random, pos.below(),
                    new ResourceLocation("aether", "chests/dungeon/silver/silver_dungeon_reward"));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureFeatureManager mgr, ChunkGenerator gen,
                            Random random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        super.postProcess(level, mgr, gen, random, box, chunkPos, pos);

        if (this.populated) {
            return;
        }
        this.populated = true;

        ServerLevel server = level.getLevel();
        if (server == null) {
            return;
        }

        BlockPos base = (this.roomOrigin != null) ? this.roomOrigin : this.templatePosition;

        double cx = base.getX() + 10.5D;
        double cy = base.getY() + 2.0D;
        double cz = base.getZ() + 12.5D;

        spawn(server, "valkyrie_queen", cx, cy, cz, true);

        int count = 3 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            double ox = cx + (random.nextDouble() - 0.5D) * 6.0D;
            double oz = cz + (random.nextDouble() - 0.5D) * 6.0D;
            spawn(server, "valkyrie", ox, cy, oz, false);
        }
    }

    private static void spawn(ServerLevel server, String path, double x, double y, double z,
                              boolean persistent) {
        EntityType<?> type = ForgeRegistries.ENTITIES.getValue(new ResourceLocation("aether", path));
        if (type == null) {
            return;
        }
        Entity e = type.create(server);
        if (e == null) {
            return;
        }
        e.moveTo(x, y, z, server.random.nextFloat() * 360.0F, 0.0F);
        if (persistent && e instanceof Mob) {
            ((Mob) e).setPersistenceRequired();
        }
        server.addFreshEntity(e);
    }
}