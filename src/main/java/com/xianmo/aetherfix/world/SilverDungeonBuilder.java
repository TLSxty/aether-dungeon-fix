package com.xianmo.aetherfix.world;

import com.xianmo.aetherfix.world.piece.SilverDungeonRoom;
import com.xianmo.aetherfix.world.piece.SilverFloorPiece;
import com.xianmo.aetherfix.world.piece.SilverTemplePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 白银地牢 3x3x3 网格布局生成器。
 *
 * 移植自 1.19.2 原版 SilverDungeonBuilder，仅使用 1.18.2 API。
 * 网格通过随机游走打洞（NORTH_DOOR / WEST_DOOR 位标记），
 * 再按位标记放置 floor / wall / door / chest_room / staircase 等片段。
 *
 * 【不要修改】以下常量与偏移公式均已通过游戏内验证：
 *   ROOM_SPACING = 7（相邻房间中心间距）
 *   FLOOR_HEIGHT = 5（层高）
 *   片段偏移公式见 assembleDungeon 内注释。
 */
public class SilverDungeonBuilder {
    private static final int CHEST_ROOM = 1;
    private static final int STAIRS = 2;
    private static final int FINAL_STAIRS = 4;
    private static final int STAIRS_MIDDLE = 8;
    private static final int STAIRS_TOP = 16;
    private static final int NORTH_DOOR = 32;
    private static final int WEST_DOOR = 64;
    private static final int VISITED = 128;

    /** 【不要修改】相邻房间中心间距，验证值 7 */
    private static final int ROOM_SPACING = 7;
    /** 【不要修改】每层高度，验证值 5 */
    private static final int FLOOR_HEIGHT = 5;

    private final Random random;
    private final int[][][] grid;
    private final int width;
    private final int height;
    private final int length;

    public SilverDungeonBuilder(Random random, int x, int y, int z) {
        this.random = random;
        this.grid = new int[x][y][z];
        this.width = x;
        this.height = y;
        this.length = z;
        this.populateGrid();
    }

    /**
     * 预置楼梯并随机游走铺满房间。
     * 楼梯从 z=0 层起逐层上升，其余空格按 1/3 概率标记为宝箱房。
     */
    private void populateGrid() {
        int finalStairsX = this.random.nextInt(this.width);
        this.grid[finalStairsX][0][0] = FINAL_STAIRS;
        this.grid[finalStairsX][1][0] = STAIRS_MIDDLE;
        this.grid[finalStairsX][2][0] = STAIRS_TOP;
        int firstStairsX = this.random.nextInt(this.width);
        this.grid[firstStairsX][0][1] = STAIRS;
        this.grid[firstStairsX][1][1] = STAIRS_TOP;
        int secondStairsX = this.random.nextInt(this.width);
        this.grid[secondStairsX][1][2] = STAIRS;
        this.grid[secondStairsX][2][2] = STAIRS_TOP;
        for (int y = 0; y < this.height; ++y) {
            this.traverseRooms(1, y, 1, 0);
            for (int z = 0; z < this.length; ++z) {
                for (int x = 0; x < this.width; ++x) {
                    if ((this.grid[x][y][z] & 0x1F) != 0 || this.random.nextInt(3) == 0) {
                        continue;
                    }
                    this.grid[x][y][z] |= CHEST_ROOM;
                }
            }
        }
    }

    /**
     * 深度优先随机游走，在相邻房间之间打通门（设置 NORTH_DOOR / WEST_DOOR 位）。
     * typesToAvoid 用于禁止某些房间类型相邻（如楼梯不能紧挨楼梯）。
     */
    private boolean traverseRooms(int x, int y, int z, int typesToAvoid) {
        if (x < 0 || x >= this.width || z < 0 || z >= this.length) {
            return false;
        }
        int room = this.grid[x][y][z];
        if ((room & typesToAvoid) > 0) {
            return false;
        }
        if ((room & VISITED) == VISITED) {
            return this.random.nextInt(3) == 0;
        }
        this.grid[x][y][z] |= VISITED;
        int blacklist = this.setNeighborBlacklist(room);
        List<Direction> directions = new ArrayList<>(4);
        Collections.addAll(directions, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST);
        for (int i = directions.size(); i > 0; --i) {
            Direction dir = directions.remove(this.random.nextInt(i));
            switch (dir) {
                case NORTH:
                    if (this.traverseRooms(x, y, z - 1, blacklist)) {
                        this.grid[x][y][z] |= NORTH_DOOR;
                    }
                    break;
                case SOUTH:
                    if (this.traverseRooms(x, y, z + 1, blacklist)) {
                        this.grid[x][y][z + 1] |= NORTH_DOOR;
                    }
                    break;
                case WEST:
                    if (this.traverseRooms(x - 1, y, z, blacklist)) {
                        this.grid[x][y][z] |= WEST_DOOR;
                    }
                    break;
                case EAST:
                    if (this.traverseRooms(x + 1, y, z, blacklist)) {
                        this.grid[x + 1][y][z] |= WEST_DOOR;
                    }
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    /** 计算相邻房间黑名单：楼梯与其上下段不可相邻 */
    private int setNeighborBlacklist(int roomType) {
        int blacklist = STAIRS | STAIRS_MIDDLE;
        if ((roomType & STAIRS_TOP) == STAIRS_TOP) {
            blacklist |= STAIRS;
        }
        if ((roomType & STAIRS) == STAIRS) {
            blacklist |= STAIRS_TOP;
        }
        return blacklist;
    }

    /**
     * 按网格逐格拼装结构片段。
     *
     * 【不要修改】以下偏移公式均已通过游戏内验证（六个实例全部正确）：
     *   origin  ：startPos 再偏移 (stepZ*5 - stepX, FLOOR_HEIGHT, -stepX*5 - stepZ)
     *   网格内 xo/zo：origin 加上 stepZ*x*7 + stepX*z*7 与 stepZ*z*7 - stepX*x*7
     *   floor  ：bp.offset(stepX + stepZ, -1, stepZ - stepX)
     *   北墙/门：bp.offset(stepZ, 0, -stepX)，用 rotation
     *   西墙/门：bp.relative(direction)，用 sideways（顺时针 90 度）
     *   高楼梯 ：bp.offset(2, 0, 2)
     *   boss 门：bp.offset(stepZ * 3, 0, -stepX * 3)
     *   楼梯   ：bp.offset(2, 0, 2)
     *   宝箱房 ：bp.offset(3, 0, 3)
     *
     * 改动任一处都会导致墙地错位、缺墙或内容物飞出骨架，请先核对技术文档第八章。
     */
    public void assembleDungeon(StructurePiecesBuilder builder, StructureManager templateManager,
                            BlockPos startPos, Rotation rotation, Direction direction) {
        BlockPos origin = startPos.offset(
                direction.getStepZ() * 5 - direction.getStepX(),
                FLOOR_HEIGHT,
                -direction.getStepX() * 5 - direction.getStepZ());
        Rotation sideways = rotation.getRotated(Rotation.CLOCKWISE_90);
        for (int y = this.height - 1; y >= 0; --y) {
            int baseY = origin.getY() + y * FLOOR_HEIGHT;
            for (int z = 0; z < this.length; ++z) {
                for (int x = 0; x < this.width; ++x) {
                    int xo = origin.getX() + direction.getStepZ() * x * ROOM_SPACING + direction.getStepX() * z * ROOM_SPACING;
                    int zo = origin.getZ() + direction.getStepZ() * z * ROOM_SPACING - direction.getStepX() * x * ROOM_SPACING;
                    BlockPos bp = new BlockPos(xo, baseY, zo);
                    int room = this.grid[x][y][z];
                    boolean northDoor = (room & NORTH_DOOR) == NORTH_DOOR;
                    boolean westDoor = (room & WEST_DOOR) == WEST_DOOR;
                    BlockPos nPos = bp.offset(direction.getStepZ(), 0, -direction.getStepX());
                    BlockPos wPos = bp.relative(direction);

                    SilverFloorPiece fp = new SilverFloorPiece(templateManager, "floor",
                            bp.offset(direction.getStepX() + direction.getStepZ(), -1, direction.getStepZ() - direction.getStepX()), rotation);
                    builder.addPiece(fp);

                    SilverTemplePiece np = new SilverTemplePiece(templateManager, northDoor ? "door" : "wall", nPos, rotation);
                    builder.addPiece(np);

                    SilverTemplePiece wp = new SilverTemplePiece(templateManager, westDoor ? "door" : "wall", wPos, sideways);
                    builder.addPiece(wp);

                    if ((room & FINAL_STAIRS) == FINAL_STAIRS) {
                        builder.addPiece(new SilverDungeonRoom(templateManager, "tall_staircase", bp.offset(2, 0, 2), rotation));
                        builder.addPiece(new SilverTemplePiece(templateManager, "boss_door",
                                bp.offset(direction.getStepZ() * 3, 0, -direction.getStepX() * 3), rotation));
                        continue;
                    }
                    if ((room & STAIRS) == STAIRS) {
                        builder.addPiece(new SilverDungeonRoom(templateManager, "staircase", bp.offset(2, 0, 2), rotation));
                        continue;
                    }
                    if ((room & CHEST_ROOM) == CHEST_ROOM) {
                        builder.addPiece(new SilverDungeonRoom(templateManager, "chest_room", bp.offset(3, 0, 3), rotation));
                    }
                }
            }
        }
    }
}