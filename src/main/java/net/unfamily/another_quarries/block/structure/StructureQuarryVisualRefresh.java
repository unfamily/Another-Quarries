package net.unfamily.another_quarries.block.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public final class StructureQuarryVisualRefresh {
    private static final ThreadLocal<Set<BlockPos>> DEFERRED_ORIGINS = ThreadLocal.withInitial(HashSet::new);

    private StructureQuarryVisualRefresh() {}

    public static void refreshAround(LevelAccessor level, BlockPos origin) {
        StructureQuarryBlock.updateConnectionsAround(level, origin);
        if (!(level instanceof Level world)) {
            return;
        }
        HashSet<BlockPos> positions = new HashSet<>();
        positions.add(origin);
        for (Direction direction : Direction.values()) {
            positions.add(origin.relative(direction));
        }
        for (BlockPos pos : positions) {
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof StructureQuarryBlock)) {
                continue;
            }
            int nodePreview = StructureQuarryBlock.effectiveNodePreviewMask(world, pos, state);
            if (StructureQuarryNodePreviewTracker.noteChanged(pos, nodePreview)) {
                world.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Queues a connection refresh for the end of the current frame-work tick. */
    public static void markDirty(BlockPos origin) {
        DEFERRED_ORIGINS.get().add(origin.immutable());
    }

    /** Applies all deferred refreshes and clears the queue. */
    public static void flushDeferred(LevelAccessor level) {
        Set<BlockPos> deferred = DEFERRED_ORIGINS.get();
        if (deferred.isEmpty()) {
            return;
        }
        for (BlockPos origin : deferred) {
            refreshAround(level, origin);
        }
        deferred.clear();
    }
}
