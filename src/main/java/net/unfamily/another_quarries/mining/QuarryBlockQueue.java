package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.util.QuarryAreaLogic;
import net.unfamily.another_quarries.util.QuarryDiggingMode;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Layer-first mining queue. Volume mode clears one full horizontal layer at a time across the area,
 * then continues below. Chunk mode does the same per chunk before advancing to the next chunk.
 */
public final class QuarryBlockQueue {
    public enum Phase {
        CLEAR_VOLUME,
        BELOW
    }

    private static final int MAX_EMPTY_LAYER_ADVANCES = 64;

    private final BlockPos quarryPos;
    private final Direction facing;
    private final int sizeLeft;
    private final int sizeRight;
    private final int sizeHeight;
    private final int sizeDepth;
    private final QuarryDiggingMode mode;
    private final LongArrayList areaChunks;

    private Phase phase;
    private int chunkIndex;
    private int activeChunkX;
    private int activeChunkZ;
    /** Current volume layer index (sizeHeight = top, 0 = base row). */
    private int volumeDy;
    /** Layers below the volume base; 0 = first row under the box. */
    private int belowLayer;
    private int layerCursor;
    private List<BlockPos> currentLayerBlocks = List.of();
    private final List<BlockPos> regenQueue = new ArrayList<>();

    public static QuarryBlockQueue empty() {
        return new QuarryBlockQueue(
                BlockPos.ZERO,
                Direction.NORTH,
                0,
                0,
                0,
                0,
                QuarryDiggingMode.VOLUME,
                new LongArrayList(),
                Phase.CLEAR_VOLUME,
                0,
                0,
                0,
                0);
    }

    private QuarryBlockQueue(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth,
            QuarryDiggingMode mode,
            LongArrayList areaChunks,
            Phase phase,
            int chunkIndex,
            int volumeDy,
            int belowLayer,
            int layerCursor) {
        this.quarryPos = quarryPos;
        this.facing = facing;
        this.sizeLeft = sizeLeft;
        this.sizeRight = sizeRight;
        this.sizeHeight = sizeHeight;
        this.sizeDepth = sizeDepth;
        this.mode = mode;
        this.areaChunks = areaChunks;
        this.phase = phase;
        this.chunkIndex = chunkIndex;
        this.volumeDy = volumeDy;
        this.belowLayer = belowLayer;
        this.layerCursor = layerCursor;
        refreshActiveChunkCoords();
        refreshCurrentLayer();
    }

    public static QuarryBlockQueue build(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth,
            QuarryDiggingMode mode,
            int chunkIndex,
            Phase phase,
            int volumeDy,
            int belowLayer,
            int layerCursor) {
        if (volumeDy < 0 && phase == Phase.CLEAR_VOLUME) {
            volumeDy = sizeHeight;
        }
        if (phase == Phase.CLEAR_VOLUME && volumeDy > sizeHeight) {
            volumeDy = sizeHeight;
        }
        LongArrayList chunks = mode == QuarryDiggingMode.CHUNK
                ? QuarryAreaLogic.enumerateAreaChunks(quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth)
                : new LongArrayList();
        return new QuarryBlockQueue(
                quarryPos,
                facing,
                sizeLeft,
                sizeRight,
                sizeHeight,
                sizeDepth,
                mode,
                chunks,
                phase,
                chunkIndex,
                volumeDy,
                belowLayer,
                layerCursor);
    }

    public static int chunkCount(
            BlockPos quarryPos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        return QuarryAreaLogic.enumerateAreaChunks(
                quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth).size();
    }

    public BlockPos takeNextMineable(Level level) {
        return takeNextMineable(level, Set.of());
    }

    public BlockPos takeNextMineable(Level level, Collection<BlockPos> reserved) {
        return takeNextMineable(level, reserved, QuarryMiningLevels.TIER_MODDED);
    }

    public BlockPos takeNextMineable(Level level, Collection<BlockPos> reserved, int maxMiningLevel) {
        Set<BlockPos> blocked = reserved.isEmpty() ? Set.of() : new HashSet<>(reserved);

        BlockPos regen = takeNextRegenMineable(level, blocked, maxMiningLevel);
        if (regen != null) {
            return regen;
        }

        int emptyAdvances = 0;
        while (emptyAdvances++ < MAX_EMPTY_LAYER_ADVANCES) {
            BlockPos pos = findNextInCurrentLayer(level, blocked, maxMiningLevel);
            if (pos != null) {
                return pos;
            }
            if (!isCurrentLayerComplete(level, blocked, maxMiningLevel)) {
                return null;
            }
            if (!advanceLayer(level)) {
                return null;
            }
            refreshCurrentLayer();
        }
        return null;
    }

    public void fastSkipAir(Level level) {
        regenQueue.removeIf(pos -> level.isEmptyBlock(pos));
    }

    private BlockPos findNextInCurrentLayer(Level level, Set<BlockPos> reserved, int maxMiningLevel) {
        for (BlockPos pos : currentLayerBlocks) {
            if (reserved.contains(pos)) {
                continue;
            }
            if (!isMineableChunkLoaded(level, pos)) {
                continue;
            }
            if (level.isEmptyBlock(pos)) {
                continue;
            }
            if (QuarryMiningFilters.isMineable(level, pos, maxMiningLevel)) {
                return pos;
            }
        }
        return null;
    }

    private boolean isCurrentLayerComplete(Level level, Set<BlockPos> reserved, int maxMiningLevel) {
        for (BlockPos pos : currentLayerBlocks) {
            if (reserved.contains(pos)) {
                continue;
            }
            if (!isMineableChunkLoaded(level, pos)) {
                return false;
            }
            if (QuarryMiningFilters.isMineable(level, pos, maxMiningLevel)) {
                return false;
            }
        }
        return true;
    }

    private boolean isMineableChunkLoaded(Level level, BlockPos pos) {
        return level.isLoaded(pos);
    }

    private boolean advanceLayer(Level level) {
        if (phase == Phase.CLEAR_VOLUME) {
            volumeDy--;
            if (volumeDy >= 0) {
                return true;
            }
            phase = Phase.BELOW;
            belowLayer = 0;
            return true;
        }

        belowLayer++;
        int minY = level.getMinY();
        int y = QuarryAreaLogic.belowVolumeStartY(quarryPos, facing) - belowLayer;
        return y >= minY;
    }

    private void refreshActiveChunkCoords() {
        if (mode != QuarryDiggingMode.CHUNK || areaChunks.isEmpty()) {
            activeChunkX = 0;
            activeChunkZ = 0;
            return;
        }
        int index = Math.min(Math.max(chunkIndex, 0), areaChunks.size() - 1);
        ChunkPos chunk = ChunkPos.unpack(areaChunks.getLong(index));
        activeChunkX = chunk.x();
        activeChunkZ = chunk.z();
    }

    private void refreshCurrentLayer() {
        if (mode == QuarryDiggingMode.CHUNK) {
            if (phase == Phase.CLEAR_VOLUME) {
                currentLayerBlocks = QuarryAreaLogic.enumerateVolumeLayerAtDyInChunk(
                        quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth,
                        volumeDy, activeChunkX, activeChunkZ);
            } else {
                int y = QuarryAreaLogic.belowVolumeStartY(quarryPos, facing) - belowLayer;
                currentLayerBlocks = QuarryAreaLogic.enumerateBelowLayerAtYInChunk(
                        quarryPos, facing, sizeLeft, sizeRight, sizeDepth, y, activeChunkX, activeChunkZ);
            }
            return;
        }

        if (phase == Phase.CLEAR_VOLUME) {
            currentLayerBlocks = QuarryAreaLogic.enumerateVolumeLayerAtDy(
                    quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth, volumeDy);
        } else {
            int y = QuarryAreaLogic.belowVolumeStartY(quarryPos, facing) - belowLayer;
            currentLayerBlocks = QuarryAreaLogic.enumerateBelowLayerAtY(
                    quarryPos, facing, sizeLeft, sizeRight, sizeDepth, y);
        }
    }

    public List<BlockPos> findRegeneratedBlocks(Level level) {
        return findRegeneratedBlocks(level, QuarryMiningLevels.TIER_MODDED);
    }

    public List<BlockPos> findRegeneratedBlocks(Level level, int maxMiningLevel) {
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> found = new ArrayList<>();

        int minVolumeDy = phase == Phase.CLEAR_VOLUME ? volumeDy : 0;
        for (int dy = sizeHeight; dy >= minVolumeDy; dy--) {
            collectMineable(level, volumeLayerPositions(dy), seen, found, maxMiningLevel);
        }

        if (phase == Phase.BELOW) {
            int startY = QuarryAreaLogic.belowVolumeStartY(quarryPos, facing);
            int minY = level.getMinY();
            for (int layer = 0; layer <= belowLayer; layer++) {
                int y = startY - layer;
                if (y < minY) {
                    break;
                }
                collectMineable(level, belowLayerPositions(y), seen, found, maxMiningLevel);
            }
        }
        return found;
    }

    private List<BlockPos> volumeLayerPositions(int dy) {
        if (mode == QuarryDiggingMode.CHUNK) {
            return QuarryAreaLogic.enumerateVolumeLayerAtDyInChunk(
                    quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth,
                    dy, activeChunkX, activeChunkZ);
        }
        return QuarryAreaLogic.enumerateVolumeLayerAtDy(
                quarryPos, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth, dy);
    }

    private List<BlockPos> belowLayerPositions(int y) {
        if (mode == QuarryDiggingMode.CHUNK) {
            return QuarryAreaLogic.enumerateBelowLayerAtYInChunk(
                    quarryPos, facing, sizeLeft, sizeRight, sizeDepth, y, activeChunkX, activeChunkZ);
        }
        return QuarryAreaLogic.enumerateBelowLayerAtY(
                quarryPos, facing, sizeLeft, sizeRight, sizeDepth, y);
    }

    private void collectMineable(
            Level level,
            List<BlockPos> positions,
            Set<BlockPos> seen,
            List<BlockPos> found,
            int maxMiningLevel) {
        for (BlockPos pos : positions) {
            if (!isMineableChunkLoaded(level, pos)) {
                continue;
            }
            if (QuarryMiningFilters.isMineable(level, pos, maxMiningLevel) && seen.add(pos)) {
                found.add(pos);
            }
        }
    }

    public void setRegenQueue(List<BlockPos> blocks) {
        regenQueue.clear();
        regenQueue.addAll(blocks);
    }

    public List<BlockPos> getRegenQueue() {
        return regenQueue;
    }

    private BlockPos takeNextRegenMineable(Level level, Set<BlockPos> reserved, int maxMiningLevel) {
        while (!regenQueue.isEmpty()) {
            BlockPos pos = regenQueue.remove(0);
            if (reserved.contains(pos)) {
                continue;
            }
            if (mode == QuarryDiggingMode.CHUNK && !isInActiveChunk(pos)) {
                continue;
            }
            if (!isMineableChunkLoaded(level, pos)) {
                continue;
            }
            if (QuarryMiningFilters.isMineable(level, pos, maxMiningLevel)) {
                return pos;
            }
        }
        return null;
    }

    private boolean isInActiveChunk(BlockPos pos) {
        if (mode != QuarryDiggingMode.CHUNK) {
            return true;
        }
        return (pos.getX() >> 4) == activeChunkX && (pos.getZ() >> 4) == activeChunkZ;
    }

    public boolean hasRemainingChunks() {
        return mode == QuarryDiggingMode.CHUNK && !areaChunks.isEmpty() && chunkIndex + 1 < areaChunks.size();
    }

    public boolean isCurrentChunkComplete(Level level) {
        return isCurrentChunkComplete(level, QuarryMiningLevels.TIER_MODDED);
    }

    public boolean isCurrentChunkComplete(Level level, int maxMiningLevel) {
        if (phase != Phase.BELOW) {
            return false;
        }
        return isBelowExhausted(level, maxMiningLevel);
    }

    public boolean isBelowExhausted(Level level) {
        return isBelowExhausted(level, QuarryMiningLevels.TIER_MODDED);
    }

    public boolean isBelowExhausted(Level level, int maxMiningLevel) {
        if (phase != Phase.BELOW) {
            return false;
        }
        int minY = level.getMinY();
        int startY = QuarryAreaLogic.belowVolumeStartY(quarryPos, facing);
        return startY - belowLayer < minY && isCurrentLayerComplete(level, Set.of(), maxMiningLevel);
    }

    public void advanceToNextChunk() {
        chunkIndex++;
        phase = Phase.CLEAR_VOLUME;
        volumeDy = sizeHeight;
        belowLayer = 0;
        layerCursor = 0;
        refreshActiveChunkCoords();
        refreshCurrentLayer();
    }

    public boolean isExhausted(Level level) {
        if (mode == QuarryDiggingMode.CHUNK) {
            return isBelowExhausted(level) && !hasRemainingChunks();
        }
        return isBelowExhausted(level);
    }

    public static boolean isMineable(Level level, BlockPos pos) {
        return QuarryMiningFilters.isMineable(level, pos);
    }

    public static boolean isMineable(Level level, BlockPos pos, int maxMiningLevel) {
        return QuarryMiningFilters.isMineable(level, pos, maxMiningLevel);
    }

    public Phase getPhase() {
        return phase;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getVolumeDy() {
        return volumeDy;
    }

    public int getBelowLayer() {
        return belowLayer;
    }

    public int getCursor() {
        return layerCursor;
    }

    public void setCursor(int cursor) {
        this.layerCursor = Math.max(0, cursor);
    }

    public int getBelowColumnIndex() {
        return layerCursor;
    }

    public int size() {
        return currentLayerBlocks.size();
    }
}
