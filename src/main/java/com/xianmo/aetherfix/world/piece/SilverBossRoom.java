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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SilverBossRoom extends SilverDungeonPiece {

    private boolean populated = false;

    private BlockPos roomOrigin;
    private BlockPos gridStart;

    /** 各可刷怪网格房间的地板点，用于把守卫女武神分散到整个地牢（由结构生成时注入）。 */
    private List<BlockPos> spawnPoints = new ArrayList<>();

    public void setGridStart(BlockPos p) {
        this.gridStart = p;
    }

    /** 由 SilverDungeonStructure 传入各可刷怪房间坐标（assembleDungeon 的返回值）。 */
    public void setSpawnPoints(List<BlockPos> points) {
        this.spawnPoints = points;
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

        // 数量 1~2（原 3~5），且把守卫女武神分散到整个 3x3x3 地牢的可刷怪房间，而非堆积在 boss 房
        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            BlockPos p = pickSpawnPoint(random);
            spawn(server, "valkyrie", p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D, false);
        }
    }

    /**
     * 从多个可刷怪房间中随机挑一个作为女武神出生点。
     * 若结构生成时注入了房间坐标则从中随机；否则回退到 boss 房门口附近。
     */
    private BlockPos pickSpawnPoint(Random random) {
        if (!this.spawnPoints.isEmpty()) {
            return this.spawnPoints.get(random.nextInt(this.spawnPoints.size()));
        }
        BlockPos base = (this.roomOrigin != null) ? this.roomOrigin : this.templatePosition;
        int bx = base.getX() + 10;
        int bz = base.getZ() + 12;
        int ox = bx + (random.nextInt(9) - 4);
        int oz = bz + (random.nextInt(9) - 4);
        int oy = (this.roomOrigin != null) ? this.roomOrigin.getY() + 2 : base.getY() + 2;
        return new BlockPos(ox, oy, oz);
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