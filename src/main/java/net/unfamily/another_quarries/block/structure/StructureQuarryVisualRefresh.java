package net.unfamily.another_quarries.block.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class StructureQuarryVisualRefresh {
    private StructureQuarryVisualRefresh() {}

    public static void refreshAround(LevelAccessor level, BlockPos origin) {
        StructureQuarryBlock.updateConnectionsAround(level, origin);
        if (!(level instanceof Level world)) {
            return;
        }
        java.util.HashSet<BlockPos> positions = new java.util.HashSet<>();
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
}
