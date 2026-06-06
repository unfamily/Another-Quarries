package net.unfamily.another_quarries.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Redstone detection for quarry machines (aligned with iskandert_utilities machine blocks).
 */
public final class QuarryRedstoneUtil {
    private static final double MAX_PASSABLE_COVER_HEIGHT = 13.0D / 16.0D;
    private static final int REDSTONE_VERTICAL_SCAN_DEPTH = 8;

    private QuarryRedstoneUtil() {}

    public static boolean hasRedstoneSignal(Level level, BlockPos pos) {
        if (level.getBestNeighborSignal(pos) > 0 || level.hasNeighborSignal(pos)) {
            return true;
        }

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (!belowState.isAir() && isPoweredRedstoneAt(level, below, belowState, Direction.UP)) {
            return true;
        }

        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (!aboveState.isAir() && isPoweredRedstoneAt(level, above, aboveState, Direction.DOWN)) {
            return true;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighbor);
            if (!neighborState.isAir() && isPoweredRedstoneAt(level, neighbor, neighborState, direction.getOpposite())) {
                return true;
            }
        }

        return scanVerticalRedstone(level, pos, Direction.UP)
                || scanVerticalRedstone(level, pos, Direction.DOWN);
    }

    private static boolean scanVerticalRedstone(Level level, BlockPos origin, Direction direction) {
        BlockPos cursor = origin.relative(direction);
        Direction towardOrigin = direction.getOpposite();
        for (int step = 0; step < REDSTONE_VERTICAL_SCAN_DEPTH; step++) {
            BlockState state = level.getBlockState(cursor);
            if (state.isAir()) {
                cursor = cursor.relative(direction);
                continue;
            }
            if (isPoweredRedstoneAt(level, cursor, state, towardOrigin)) {
                return true;
            }
            if (!isPassableRedstoneCover(level, cursor, state)) {
                break;
            }
            cursor = cursor.relative(direction);
        }
        return false;
    }

    private static boolean isPoweredRedstoneAt(Level level, BlockPos pos, BlockState state, Direction towardTarget) {
        if (state.is(Blocks.REDSTONE_WIRE)) {
            return state.getValue(RedStoneWireBlock.POWER) > 0;
        }
        if (state.is(Blocks.REDSTONE_BLOCK)) {
            return true;
        }
        if (level.getControlInputSignal(pos, towardTarget, false) > 0) {
            return true;
        }
        if (level.getSignal(pos, towardTarget) > 0) {
            return true;
        }
        return state.isSignalSource() && state.getSignal(level, pos, towardTarget) > 0;
    }

    private static boolean isPassableRedstoneCover(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (state.isSolidRender(level, pos)) {
            return false;
        }
        VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.empty());
        if (shape.isEmpty()) {
            return true;
        }
        AABB bounds = shape.bounds();
        double height = bounds.maxY - bounds.minY;
        return height <= MAX_PASSABLE_COVER_HEIGHT;
    }
}
