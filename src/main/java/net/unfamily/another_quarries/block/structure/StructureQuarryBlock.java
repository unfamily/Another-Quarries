package net.unfamily.another_quarries.block.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.another_quarries.client.DuctShapes;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class StructureQuarryBlock extends Block implements SimpleWaterloggedBlock {
    public static final IntegerProperty CONNECTIONS = IntegerProperty.create("connections", 0, 63);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public StructureQuarryBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CONNECTIONS, 0).setValue(WATERLOGGED, false));
    }

    public static int connectionMask(BlockState state) {
        return state.hasProperty(CONNECTIONS) ? state.getValue(CONNECTIONS) : 0;
    }

    public static int effectivePipeMask(BlockState state) {
        return connectionMask(state);
    }

    public static int effectiveNodePreviewMask(BlockGetter level, BlockPos pos, BlockState state) {
        return computeNodePreviewMask(level, pos);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int nodes = effectiveNodePreviewMask(level, pos, state);
        return DuctShapes.forMasks(effectivePipeMask(state), nodes);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Override
    protected SoundType getSoundType(BlockState state) {
        return SoundType.COPPER;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        StructureQuarryVisualRefresh.refreshAround(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston) {
            StructureQuarryNodePreviewTracker.remove(pos);
            StructureQuarryVisualRefresh.refreshAround(level, pos);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            net.minecraft.world.level.redstone.@org.jspecify.annotations.Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        StructureQuarryVisualRefresh.refreshAround(level, pos);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            net.minecraft.world.level.LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state.setValue(CONNECTIONS, computeConnectionMask(level, pos));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
        BlockState placed = defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
        return placed.setValue(CONNECTIONS, computeConnectionMask(ctx.getLevel(), ctx.getClickedPos()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTIONS, WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public static int computeConnectionMask(BlockGetter level, BlockPos pos) {
        if (level instanceof Level world) {
            int mask = 0;
            for (Direction direction : Direction.values()) {
                if (StructureQuarryVisualAdjacency.connectsPipeVisually(world, pos, direction)) {
                    mask |= 1 << direction.ordinal();
                }
            }
            return mask;
        }
        int mask = 0;
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (StructureQuarryNetwork.isStructureQuarry(neighbor.getBlock())
                    || StructureQuarryVisualAdjacency.isQuarryNeighbor(neighbor)) {
                mask |= 1 << direction.ordinal();
            }
        }
        return mask;
    }

    public static int computeNodePreviewMask(BlockGetter level, BlockPos pos) {
        int mask = 0;
        for (Direction direction : Direction.values()) {
            if (StructureQuarryVisualAdjacency.connectsNodePreview(level, pos, direction)) {
                mask |= 1 << direction.ordinal();
            }
        }
        return mask;
    }

    public static void updateConnectionsAround(LevelAccessor level, BlockPos origin) {
        Set<BlockPos> positions = new HashSet<>();
        positions.add(origin);
        for (Direction direction : Direction.values()) {
            positions.add(origin.relative(direction));
        }
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof StructureQuarryBlock)) {
                continue;
            }
            int pipe = computeConnectionMask(level, pos);
            if (state.getValue(CONNECTIONS) != pipe) {
                level.setBlock(pos, state.setValue(CONNECTIONS, pipe), Block.UPDATE_CLIENTS);
            }
        }
    }
}
