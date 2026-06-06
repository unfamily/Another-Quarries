package net.unfamily.another_quarries.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.unfamily.another_quarries.config.ModConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
     * True when a frame outline makes sense (volume is at least 2 blocks on each horizontal axis).
     */
    public static boolean hasFrameOutline(int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth) {
        return blockWidth(sizeLeft, sizeRight) >= 2
                && blockDepth(sizeDepth) >= 2
                && blockHeight(sizeHeight) >= 1;
    }

    /** Chunk-by-chunk mining when either horizontal axis exceeds {@link ModConfig#volumeModeMaxFootprint()}. */
    public static boolean requiresChunkDiggingMode(int sizeLeft, int sizeRight, int sizeDepth) {
        int maxFootprint = ModConfig.volumeModeMaxFootprint();
        return blockWidth(sizeLeft, sizeRight) > maxFootprint
                || blockDepth(sizeDepth) > maxFootprint;
    }

    /**
     * Decorative frame: four corner pillars plus a thin outer rim at floor and ceiling.
     */
    public static List<BlockPos> enumerateFrameStructureBlocks(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        List<BlockPos> positions = new ArrayList<>();
        forEachFrameStructureBlock(
                quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth, positions::add);
        return positions;
    }

    /**
     * Visits frame block positions directly (O(frame size), not O(volume)).
     */
    public static void forEachFrameStructureBlock(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth,
            Consumer<BlockPos> consumer) {
        visitFrameStructureBlocks(
                quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth,
                pos -> {
                    consumer.accept(pos);
                    return true;
                });
    }

    private static boolean visitFrameStructureBlocks(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth,
            java.util.function.Predicate<BlockPos> visitor) {
        if (!hasFrameOutline(sizeLeft, sizeRight, sizeHeight, sizeDepth)) {
            return true;
        }
        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise(Direction.Axis.Y);
        Direction right = facing.getClockWise(Direction.Axis.Y);
        BlockPos base = miningBase(quarryPos, facing);

        int[][] corners = {
                {sizeLeft, 0, 0},
                {0, sizeRight, 0},
                {sizeLeft, 0, sizeDepth},
                {0, sizeRight, sizeDepth}
        };
        for (int[] corner : corners) {
            for (int dy = 0; dy <= sizeHeight; dy++) {
                if (!visitor.test(offsetFromBase(base, back, left, right, corner[0], corner[1], dy, corner[2]))) {
                    return false;
                }
            }
        }

        for (int dy : new int[] {0, sizeHeight}) {
            for (int dd = 0; dd <= sizeDepth; dd++) {
                if (!visitor.test(offsetFromBase(base, back, left, right, sizeLeft, 0, dy, dd))) {
                    return false;
                }
                if (!visitor.test(offsetFromBase(base, back, left, right, 0, sizeRight, dy, dd))) {
                    return false;
                }
            }
            for (int dl = 0; dl <= sizeLeft; dl++) {
                if (!visitor.test(offsetFromBase(base, back, left, right, dl, 0, dy, 0))) {
                    return false;
                }
                if (!visitor.test(offsetFromBase(base, back, left, right, dl, 0, dy, sizeDepth))) {
                    return false;
                }
            }
            for (int dr = 0; dr <= sizeRight; dr++) {
                if (!visitor.test(offsetFromBase(base, back, left, right, 0, dr, dy, 0))) {
                    return false;
                }
                if (!visitor.test(offsetFromBase(base, back, left, right, 0, dr, dy, sizeDepth))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Builds a set of frame positions for fast membership checks during stray-block scans. */
    public static Set<BlockPos> frameStructureBlockSet(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        Set<BlockPos> positions = new HashSet<>();
        forEachFrameStructureBlock(
                quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth, positions::add);
        return positions;
    }

    /** Number of frame positions in visit order (same order as {@link #forEachFrameStructureBlockSlice}). */
    public static int frameStructureBlockCount(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        int[] count = {0};
        forEachFrameStructureBlock(
                quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth, pos -> count[0]++);
        return count[0];
    }

    /**
     * Visits up to {@code budget} frame positions without building a list.
     *
     * @return index to pass as {@code startIndex} on the next call
     */
    public static int forEachFrameStructureBlockSlice(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth,
            int startIndex,
            int budget,
            Consumer<BlockPos> consumer) {
        if (budget <= 0 || startIndex < 0) {
            return startIndex;
        }
        int[] index = {0};
        int[] nextIndex = {startIndex};
        int[] remaining = {budget};
        visitFrameStructureBlocks(
                quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth,
                pos -> {
                    if (remaining[0] <= 0) {
                        return false;
                    }
                    if (index[0] < startIndex) {
                        index[0]++;
                        return true;
                    }
                    consumer.accept(pos);
                    nextIndex[0] = ++index[0];
                    remaining[0]--;
                    return true;
                });
        return nextIndex[0];
    }

    /** Border-shell positions excluding valid frame blocks (for stray {@code structure_quarry} cleanup). */
    public static int borderShellBlockCount(
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        if (!hasBorderShell(sizeLeft, sizeRight, sizeDepth)) {
            return 0;
        }
        int count = 0;
        for (int dd = 0; dd <= sizeDepth; dd++) {
            for (int dy = 0; dy <= sizeHeight; dy++) {
                for (int dl = 0; dl <= sizeLeft; dl++) {
                    for (int dr = 0; dr <= sizeRight; dr++) {
                        if (isBorderShell(dl, dr, dd, dy, sizeLeft, sizeRight, sizeDepth, sizeHeight)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    /**
     * Visits up to {@code budget} border-shell positions without building a list.
     *
     * @return index to pass as {@code startIndex} on the next call
     */
    public static int forEachBorderShellBlockSlice(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth,
            int startIndex,
            int budget,
            Consumer<BlockPos> consumer) {
        if (!hasBorderShell(sizeLeft, sizeRight, sizeDepth) || budget <= 0 || startIndex < 0) {
            return startIndex;
        }
        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise(Direction.Axis.Y);
        Direction right = facing.getClockWise(Direction.Axis.Y);
        BlockPos base = miningBase(quarryPos, facing);

        int[] index = {0};
        int[] nextIndex = {startIndex};
        int[] remaining = {budget};

        for (int dd = 0; dd <= sizeDepth && remaining[0] > 0; dd++) {
            for (int dy = 0; dy <= sizeHeight && remaining[0] > 0; dy++) {
                for (int dl = 0; dl <= sizeLeft && remaining[0] > 0; dl++) {
                    for (int dr = 0; dr <= sizeRight && remaining[0] > 0; dr++) {
                        if (!isBorderShell(dl, dr, dd, dy, sizeLeft, sizeRight, sizeDepth, sizeHeight)) {
                            continue;
                        }
                        if (index[0] < startIndex) {
                            index[0]++;
                            continue;
                        }
                        consumer.accept(offsetFromBase(base, back, left, right, dl, dr, dy, dd));
                        nextIndex[0] = ++index[0];
                        remaining[0]--;
                    }
                }
            }
        }
        return nextIndex[0];
    }

    /**
     * True when the configured footprint has a horizontal border shell (at least one axis wider than 2 blocks).
     */
    public static boolean hasBorderShell(int sizeLeft, int sizeRight, int sizeDepth) {
        return blockWidth(sizeLeft, sizeRight) > 2 || blockDepth(sizeDepth) > 2;
    }

    /**
     * All border-shell block positions in the configured volume (full 1-block-thick mining exclusion shell).
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
        for (BlockPos pos : enumerateFrameStructureBlocks(
                quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth)) {
            chunks.add(net.minecraft.world.level.ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
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
     * Visits up to {@code budget} interior volume positions on one dy, without building a layer list.
     *
     * @return index to pass as {@code startIndex} on the next call for the same dy
     */
    public static int forEachInteriorVolumeAtDySlice(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth,
            int dy,
            int chunkX,
            int chunkZ,
            boolean limitToChunk,
            int startIndex,
            int budget,
            Consumer<BlockPos> consumer) {
        if (dy < 0 || dy > sizeHeight || budget <= 0 || startIndex < 0) {
            return startIndex;
        }
        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise(Direction.Axis.Y);
        Direction right = facing.getClockWise(Direction.Axis.Y);
        BlockPos base = miningBase(quarryPos, facing);

        int[] index = {0};
        int[] nextIndex = {startIndex};
        int[] remaining = {budget};

        for (int dd = 0; dd <= sizeDepth && remaining[0] > 0; dd++) {
            for (int dl = 0; dl <= sizeLeft && remaining[0] > 0; dl++) {
                for (int dr = 0; dr <= sizeRight && remaining[0] > 0; dr++) {
                    if (isBorderShell(dl, dr, dd, dy, sizeLeft, sizeRight, sizeDepth, sizeHeight)) {
                        continue;
                    }
                    BlockPos pos = offsetFromBase(base, back, left, right, dl, dr, dy, dd);
                    if (limitToChunk && ((pos.getX() >> 4) != chunkX || (pos.getZ() >> 4) != chunkZ)) {
                        continue;
                    }
                    if (index[0] < startIndex) {
                        index[0]++;
                        continue;
                    }
                    consumer.accept(pos);
                    nextIndex[0] = ++index[0];
                    remaining[0]--;
                }
            }
        }
        return nextIndex[0];
    }

    /** Interior column count on one below-Y layer (respects chunk filter when enabled). */
    public static int interiorBelowLayerBlockCount(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeDepth,
            int y,
            int chunkX,
            int chunkZ,
            boolean limitToChunk) {
        int count = 0;
        for (InteriorColumn column : enumerateInteriorColumns(sizeLeft, sizeRight, sizeDepth)) {
            BlockPos pos = columnBlockAtY(quarryPos, facing, column, y);
            if (limitToChunk && ((pos.getX() >> 4) != chunkX || (pos.getZ() >> 4) != chunkZ)) {
                continue;
            }
            count++;
        }
        return count;
    }

    /**
     * Visits up to {@code budget} interior below-layer positions without building a list.
     */
    public static int forEachInteriorBelowAtYSlice(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeDepth,
            int y,
            int chunkX,
            int chunkZ,
            boolean limitToChunk,
            int startIndex,
            int budget,
            Consumer<BlockPos> consumer) {
        if (budget <= 0 || startIndex < 0) {
            return startIndex;
        }
        int[] index = {0};
        int[] nextIndex = {startIndex};
        int[] remaining = {budget};
        for (InteriorColumn column : enumerateInteriorColumns(sizeLeft, sizeRight, sizeDepth)) {
            if (remaining[0] <= 0) {
                break;
            }
            BlockPos pos = columnBlockAtY(quarryPos, facing, column, y);
            if (limitToChunk && ((pos.getX() >> 4) != chunkX || (pos.getZ() >> 4) != chunkZ)) {
                continue;
            }
            if (index[0] < startIndex) {
                index[0]++;
                continue;
            }
            consumer.accept(pos);
            nextIndex[0] = ++index[0];
            remaining[0]--;
        }
        return nextIndex[0];
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
            chunks.add(net.minecraft.world.level.ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
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

    /** Every block position inside the configured mining volume box. */
    public static List<BlockPos> enumerateOuterVolumeBlocks(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        return enumerateOuterBlocks(quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth);
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

    /**
     * Frame block: one vertical pillar per footprint corner, plus a 1-block-thick outer rim at floor/ceiling.
     */
    static boolean isFrameStructureBlock(
            int dl,
            int dr,
            int dd,
            int dy,
            int sizeLeft,
            int sizeRight,
            int sizeDepth,
            int sizeHeight) {
        if (isFrameCornerColumn(dl, dr, dd, sizeLeft, sizeRight, sizeDepth)) {
            return true;
        }
        return (dy == 0 || dy == sizeHeight)
                && isFrameTopBottomRim(dl, dr, dd, sizeLeft, sizeRight, sizeDepth);
    }

    /** One of the four horizontal corners of the configured mining footprint. */
    static boolean isFrameCornerColumn(int dl, int dr, int dd, int sizeLeft, int sizeRight, int sizeDepth) {
        return (dl == sizeLeft && dr == 0 && dd == 0)
                || (dl == 0 && dr == sizeRight && dd == 0)
                || (dl == sizeLeft && dr == 0 && dd == sizeDepth)
                || (dl == 0 && dr == sizeRight && dd == sizeDepth);
    }

    /**
     * Outer horizontal perimeter at floor/ceiling only (left/right edge lines plus front/back L-rims).
     * Does not fill the interior of front/back depth faces.
     */
    static boolean isFrameTopBottomRim(int dl, int dr, int dd, int sizeLeft, int sizeRight, int sizeDepth) {
        if (dl == sizeLeft && dr == 0) {
            return true;
        }
        if (dl == 0 && dr == sizeRight) {
            return true;
        }
        if (dd == 0 && (dr == 0 || dl == 0)) {
            return true;
        }
        return dd == sizeDepth && (dr == 0 || dl == 0);
    }

    public static BlockPos miningBase(BlockPos quarryPos, Direction facing) {
        return quarryPos.relative(facing.getOpposite(), 1);
    }

    public static BlockPos offsetFromBaseForScan(
            BlockPos base,
            Direction back,
            Direction left,
            Direction right,
            int dl,
            int dr,
            int dy,
            int dd) {
        return offsetFromBase(base, back, left, right, dl, dr, dy, dd);
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
