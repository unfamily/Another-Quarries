package net.unfamily.another_quarries.block.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.another_quarries.block.QuarryBlock;
import net.unfamily.another_quarries.registry.ModBlocks;

/**
 * Visual adjacency for quarry structure frames: connect to other structure blocks and the quarry block.
 */
public final class StructureQuarryVisualAdjacency {
    private StructureQuarryVisualAdjacency() {}

    public static boolean isQuarryNeighbor(BlockState neighborState) {
        return neighborState.getBlock() == ModBlocks.QUARRY.get();
    }

    public static boolean isVisualPipeLink(Level level, BlockPos from, Direction fromToNeighbor) {
        BlockPos to = from.relative(fromToNeighbor);
        BlockState toState = level.getBlockState(to);
        return StructureQuarryNetwork.isStructureQuarry(toState.getBlock()) || isQuarryNeighbor(toState);
    }

    public static boolean connectsPipeVisually(Level level, BlockPos pos, Direction dir) {
        return isVisualPipeLink(level, pos, dir);
    }

    /** Node voxel toward an adjacent quarry controller (not toward other structure pieces). */
    public static boolean connectsNodePreview(BlockGetter level, BlockPos pos, Direction dir) {
        BlockState neighbor = level.getBlockState(pos.relative(dir));
        if (StructureQuarryNetwork.isStructureQuarry(neighbor.getBlock())) {
            return false;
        }
        return neighbor.getBlock() instanceof QuarryBlock;
    }
}
