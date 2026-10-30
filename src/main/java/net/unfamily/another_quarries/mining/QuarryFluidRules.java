package net.unfamily.another_quarries.mining;

import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Per-tick snapshot of how a quarry treats source fluids it meets while mining.
 * Priority: filter destroy list (void) first, then pump drain list (fill tank), otherwise the fluid is skipped.
 */
public record QuarryFluidRules(
        boolean voidEnabled,
        List<String> voidFilters,
        boolean pumpEnabled,
        List<String> drainFilters,
        @Nullable Sink sink) {

    public static final QuarryFluidRules NONE = new QuarryFluidRules(false, List.of(), false, List.of(), null);

    /** What the quarry does with one fluid source block. */
    public enum Action {
        /** Fluid is ignored (not mineable). */
        NONE,
        /** Fluid is removed without filling a tank. */
        VOID,
        /** Fluid fills a tank, then is removed. */
        PUMP,
        /** Fluid should be pumped but every suitable tank is full: the worker waits. */
        WAIT
    }

    /** Tank storage the pump fills. */
    public interface Sink {
        boolean canAccept(Fluid fluid, int amountMb);

        boolean accept(Fluid fluid, int amountMb);
    }

    public boolean isActive() {
        return (voidEnabled && !voidFilters.isEmpty()) || (pumpEnabled && !drainFilters.isEmpty() && sink != null);
    }

    public Action classify(Fluid fluid) {
        if (voidEnabled && QuarryFluidFilterMatcher.matchesAny(voidFilters, fluid)) {
            return Action.VOID;
        }
        if (pumpEnabled && sink != null && QuarryFluidFilterMatcher.matchesAny(drainFilters, fluid)) {
            return sink.canAccept(fluid, QuarryFluidMining.SOURCE_AMOUNT_MB) ? Action.PUMP : Action.WAIT;
        }
        return Action.NONE;
    }
}
