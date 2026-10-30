package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Fluid handling shared by mining filters and the block breaker. The active rules are scoped to the quarry
 * currently ticking (server thread), because {@link QuarryMiningFilters} is static and queue-wide.
 */
public final class QuarryFluidMining {
    /** Volume of one fluid source block. */
    public static final int SOURCE_AMOUNT_MB = 1000;

    private static QuarryFluidRules active = QuarryFluidRules.NONE;

    private QuarryFluidMining() {}

    public static void activate(QuarryFluidRules rules) {
        active = rules != null ? rules : QuarryFluidRules.NONE;
    }

    public static void deactivate() {
        active = QuarryFluidRules.NONE;
    }

    public static QuarryFluidRules active() {
        return active;
    }

    /** Action for a pure fluid block (source only; flowing fluid is never handled). */
    public static QuarryFluidRules.Action classifyFluidBlock(BlockState state, QuarryFluidRules rules) {
        if (!(state.getBlock() instanceof LiquidBlock) || rules == null || !rules.isActive()) {
            return QuarryFluidRules.Action.NONE;
        }
        FluidState fluidState = state.getFluidState();
        if (!fluidState.isSource()) {
            return QuarryFluidRules.Action.NONE;
        }
        return rules.classify(fluidState.getType());
    }

    /**
     * Action for a source fluid held inside a solid block (waterlogged). Waterlogged blocks never wait:
     * when the fluid cannot be handled the block is mined and the fluid is left behind.
     */
    public static QuarryFluidRules.Action classifyWaterlogged(BlockState state, QuarryFluidRules rules) {
        if (state.getBlock() instanceof LiquidBlock || rules == null || !rules.isActive()) {
            return QuarryFluidRules.Action.NONE;
        }
        FluidState fluidState = state.getFluidState();
        if (fluidState.isEmpty() || !fluidState.isSource()) {
            return QuarryFluidRules.Action.NONE;
        }
        QuarryFluidRules.Action action = rules.classify(fluidState.getType());
        return action == QuarryFluidRules.Action.WAIT ? QuarryFluidRules.Action.NONE : action;
    }

    /** True when the quarry must keep waiting on this target because tanks cannot take its fluid. */
    public static boolean isWaiting(ServerLevel level, BlockPos pos, QuarryFluidRules rules) {
        BlockState state = level.getBlockState(pos);
        return classifyFluidBlock(state, rules) == QuarryFluidRules.Action.WAIT;
    }

    /**
     * Handles a pure fluid block target: voids it or pumps it into a tank.
     * @return true when the fluid block was removed
     */
    public static boolean handleFluidBlock(ServerLevel level, BlockPos pos, QuarryFluidRules rules) {
        BlockState state = level.getBlockState(pos);
        QuarryFluidRules.Action action = classifyFluidBlock(state, rules);
        if (action != QuarryFluidRules.Action.VOID && action != QuarryFluidRules.Action.PUMP) {
            return false;
        }
        var fluid = state.getFluidState().getType();
        if (action == QuarryFluidRules.Action.PUMP
                && (rules.sink() == null || !rules.sink().accept(fluid, SOURCE_AMOUNT_MB))) {
            return false;
        }
        return level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }
}
