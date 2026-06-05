package net.unfamily.another_quarries.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Mining volume behind the quarry facing. Height extends upward from the base row.
 * Phase 1 clears the interior of that box; phase 2 mines downward below the base footprint.
 */
public final class QuarryAreaLogic {

    public record InteriorColumn(int dl, int dr, int dd) {}

    private QuarryAreaLogic() {}

    public static int blockWidth(int sizeLeft, int sizeRight) {
        return sizeLeft + sizeRight + 1;
    }

    public static int blockHeight(int sizeHeight) {
        return sizeHeight + 1;
    }

    public static int blockDepth(int sizeDepth) {
        return sizeDepth + 1;
    }

    /**
     * Outer AABB for preview markers (full configured box including border shell).
     */
    public static AABB getMiningVolumeAABB(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : enumerateOuterBlocks(quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth)) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        if (minX == Integer.MAX_VALUE) {
            BlockPos base = miningBase(quarryPos, facing);
            return new AABB(base.getX(), base.getY(), base.getZ(), base.getX() + 1, base.getY() + 1, base.getZ() + 1);
        }
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    /**
     * True when the configured footprint has a horizontal border shell (at least one axis wider than 2 blocks).
     */
    public static boolean hasBorderShell(int sizeLeft, int sizeRight, int sizeDepth) {
        return blockWidth(sizeLeft, sizeRight) > 2 || blockDepth(sizeDepth) > 2;
    }

    /**
     * All border-shell block positions in the configured volume (frame perimeter).
     */
    public static List<BlockPos> enumerateBorderShellBlocks(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        if (!hasBorderShell(sizeLeft, sizeRight, sizeDepth)) {
            return List.of();
        }
        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise(Direction.Axis.Y);
        Direction right = facing.getClockWise(Direction.Axis.Y);
        BlockPos base = miningBase(quarryPos, facing);

        List<BlockPos> positions = new ArrayList<>();
        for (int dd = 0; dd <= sizeDepth; dd++) {
            for (int dy = 0; dy <= sizeHeight; dy++) {
                for (int dl = 0; dl <= sizeLeft; dl++) {
                    for (int dr = 0; dr <= sizeRight; dr++) {
                        if (isBorderShell(dl, dr, dd, dy, sizeLeft, sizeRight, sizeDepth, sizeHeight)) {
                            positions.add(offsetFromBase(base, back, left, right, dl, dr, dy, dd));
                        }
                    }
                }
            }
        }
        return positions;
    }

    /**
     * Border-shell positions on one horizontal layer (one dy).
     */
    public static List<BlockPos> enumerateBorderShellLayerAtDy(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth,
            int dy) {
        if (dy < 0 || dy > sizeHeight || !hasBorderShell(sizeLeft, sizeRight, sizeDepth)) {
            return List.of();
        }
        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise(Direction.Axis.Y);
        Direction right = facing.getClockWise(Direction.Axis.Y);
        BlockPos base = miningBase(quarryPos, facing);

        List<BlockPos> positions = new ArrayList<>();
        for (int dd = 0; dd <= sizeDepth; dd++) {
            for (int dl = 0; dl <= sizeLeft; dl++) {
                for (int dr = 0; dr <= sizeRight; dr++) {
                    if (isBorderShell(dl, dr, dd, dy, sizeLeft, sizeRight, sizeDepth, sizeHeight)) {
                        positions.add(offsetFromBase(base, back, left, right, dl, dr, dy, dd));
                    }
                }
            }
        }
        return positions;
    }

    /**
     * Distinct chunks touched by the border-shell frame footprint.
     */
    public static it.unimi.dsi.fastutil.longs.LongArrayList enumerateFrameAreaChunks(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        it.unimi.dsi.fastutil.longs.LongOpenHashSet chunks = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        for (BlockPos pos : enumerateBorderShellBlocks(
                quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth)) {
            chunks.add(net.minecraft.world.level.ChunkPos.pack(pos));
        }
        long[] packed = chunks.toLongArray();
        java.util.Arrays.sort(packed);
        return it.unimi.dsi.fastutil.longs.LongArrayList.wrap(packed);
    }

    /**
     * All interior blocks on a single horizontal layer inside the configured volume (one dy).
     */
    public static List<BlockPos> enumerateVolumeLayerAtDy(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth,
            int dy) {
        if (dy < 0 || dy > sizeHeight) {
            return List.of();
        }
        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise(Direction.Axis.Y);
        Direction right = facing.getClockWise(Direction.Axis.Y);
        BlockPos base = miningBase(quarryPos, facing);

        List<BlockPos> positions = new ArrayList<>();
        for (int dd = 0; dd <= sizeDepth; dd++) {
            for (int dl = 0; dl <= sizeLeft; dl++) {
                for (int dr = 0; dr <= sizeRight; dr++) {
                    if (isBorderShell(dl, dr, dd, dy, sizeLeft, sizeRight, sizeDepth, sizeHeight)) {
                        continue;
                    }
                    positions.add(offsetFromBase(base, back, left, right, dl, dr, dy, dd));
                }
            }
        }
        return positions;
    }

    /**
     * All interior blocks on a single world-Y layer below the configured volume.
     */
    public static List<BlockPos> enumerateBelowLayerAtY(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeDepth,
            int y) {
        List<BlockPos> positions = new ArrayList<>();
        for (InteriorColumn column : enumerateInteriorColumns(sizeLeft, sizeRight, sizeDepth)) {
            positions.add(columnBlockAtY(quarryPos, facing, column, y));
        }
        return positions;
    }

    /**
     * Phase 1: clear the configured interior volume, top-to-bottom (highest Y first).
     */
    public static List<BlockPos> enumerateInitialVolumeBlocks(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        List<BlockPos> positions = new ArrayList<>();
        for (int dy = sizeHeight; dy >= 0; dy--) {
            positions.addAll(enumerateVolumeLayerAtDy(
                    quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth, dy));
        }
        return positions;
    }

    public static List<InteriorColumn> enumerateInteriorColumns(
            int sizeLeft,
            int sizeRight,
            int sizeDepth) {
        List<InteriorColumn> columns = new ArrayList<>();
        for (int dd = 0; dd <= sizeDepth; dd++) {
            for (int dl = 0; dl <= sizeLeft; dl++) {
                for (int dr = 0; dr <= sizeRight; dr++) {
                    if (isHorizontalBorderShell(dl, dr, dd, sizeLeft, sizeRight, sizeDepth)) {
                        continue;
                    }
                    columns.add(new InteriorColumn(dl, dr, dd));
                }
            }
        }
        return columns;
    }

    public static BlockPos columnBlockAtY(
            BlockPos quarryPos,
            Direction facing,
            InteriorColumn column,
            int y) {
        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise(Direction.Axis.Y);
        Direction right = facing.getClockWise(Direction.Axis.Y);
        BlockPos base = miningBase(quarryPos, facing);
        int x = base.getX() + left.getStepX() * column.dl() + right.getStepX() * column.dr() + back.getStepX() * column.dd();
        int z = base.getZ() + left.getStepZ() * column.dl() + right.getStepZ() * column.dr() + back.getStepZ() * column.dd();
        return new BlockPos(x, y, z);
    }

    /** Y of the first row below the configured volume (base row). */
    public static int belowVolumeStartY(BlockPos quarryPos, Direction facing) {
        return miningBase(quarryPos, facing).getY() - 1;
    }

    /**
     * Distinct chunks touched by the interior mining footprint (one horizontal slice is enough for XZ).
     */
    public static it.unimi.dsi.fastutil.longs.LongArrayList enumerateAreaChunks(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        int dy = Math.min(sizeHeight, Math.max(0, sizeHeight / 2));
        it.unimi.dsi.fastutil.longs.LongOpenHashSet chunks = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        for (BlockPos pos : enumerateVolumeLayerAtDy(
                quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth, dy)) {
            chunks.add(net.minecraft.world.level.ChunkPos.pack(pos));
        }
        long[] packed = chunks.toLongArray();
        java.util.Arrays.sort(packed);
        return it.unimi.dsi.fastutil.longs.LongArrayList.wrap(packed);
    }

    public static List<BlockPos> enumerateVolumeLayerAtDyInChunk(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth,
            int dy,
            int chunkX,
            int chunkZ) {
        if (dy < 0 || dy > sizeHeight) {
            return List.of();
        }
        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise(Direction.Axis.Y);
        Direction right = facing.getClockWise(Direction.Axis.Y);
        BlockPos base = miningBase(quarryPos, facing);

        List<BlockPos> positions = new ArrayList<>();
        for (int dd = 0; dd <= sizeDepth; dd++) {
            for (int dl = 0; dl <= sizeLeft; dl++) {
                for (int dr = 0; dr <= sizeRight; dr++) {
                    if (isBorderShell(dl, dr, dd, dy, sizeLeft, sizeRight, sizeDepth, sizeHeight)) {
                        continue;
                    }
                    BlockPos pos = offsetFromBase(base, back, left, right, dl, dr, dy, dd);
                    if ((pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ) {
                        positions.add(pos);
                    }
                }
            }
        }
        return positions;
    }

    public static List<BlockPos> enumerateBelowLayerAtYInChunk(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeDepth,
            int y,
            int chunkX,
            int chunkZ) {
        List<BlockPos> positions = new ArrayList<>();
        for (InteriorColumn column : enumerateInteriorColumns(sizeLeft, sizeRight, sizeDepth)) {
            BlockPos pos = columnBlockAtY(quarryPos, facing, column, y);
            if ((pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ) {
                positions.add(pos);
            }
        }
        return positions;
    }

    private static List<BlockPos> enumerateOuterBlocks(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise(Direction.Axis.Y);
        Direction right = facing.getClockWise(Direction.Axis.Y);
        BlockPos base = miningBase(quarryPos, facing);

        List<BlockPos> positions = new ArrayList<>();
        for (int dd = 0; dd <= sizeDepth; dd++) {
            for (int dy = 0; dy <= sizeHeight; dy++) {
                for (int dl = 0; dl <= sizeLeft; dl++) {
                    for (int dr = 0; dr <= sizeRight; dr++) {
                        positions.add(offsetFromBase(base, back, left, right, dl, dr, dy, dd));
                    }
                }
            }
        }
        return positions;
    }

    static boolean isBorderShell(
            int dl,
            int dr,
            int dd,
            int dy,
            int sizeLeft,
            int sizeRight,
            int sizeDepth,
            int sizeHeight) {
        return isHorizontalBorderShell(dl, dr, dd, sizeLeft, sizeRight, sizeDepth);
    }

    static boolean isHorizontalBorderShell(int dl, int dr, int dd, int sizeLeft, int sizeRight, int sizeDepth) {
        boolean widthBorder = blockWidth(sizeLeft, sizeRight) > 2 && (dl == sizeLeft || dr == sizeRight);
        boolean depthBorder = blockDepth(sizeDepth) > 2 && (dd == 0 || dd == sizeDepth);
        return widthBorder || depthBorder;
    }

    static BlockPos miningBase(BlockPos quarryPos, Direction facing) {
        return quarryPos.relative(facing.getOpposite(), 1);
    }

    private static BlockPos offsetFromBase(
            BlockPos base,
            Direction back,
            Direction left,
            Direction right,
            int dl,
            int dr,
            int dy,
            int dd) {
        int x = base.getX() + left.getStepX() * dl + right.getStepX() * dr + back.getStepX() * dd;
        int y = base.getY() + dy;
        int z = base.getZ() + left.getStepZ() * dl + right.getStepZ() * dr + back.getStepZ() * dd;
        return new BlockPos(x, y, z);
    }
}
