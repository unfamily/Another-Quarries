package net.unfamily.another_quarries.block.entity;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** External automation view over the quarry tanks: extract only; fills are rejected. */
public final class ExtractOnlyFluidHandler implements IFluidHandler {
    private final FluidTank[] tanks;

    public ExtractOnlyFluidHandler(FluidTank[] tanks) {
        this.tanks = tanks;
    }

    @Override
    public int getTanks() {
        return tanks.length;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tanks[tank].getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tanks[tank].getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        for (FluidTank tank : tanks) {
            FluidStack drained = tank.drain(resource, action);
            if (!drained.isEmpty()) {
                return drained;
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        for (FluidTank tank : tanks) {
            if (!tank.getFluid().isEmpty()) {
                FluidStack drained = tank.drain(maxDrain, action);
                if (!drained.isEmpty()) {
                    return drained;
                }
            }
        }
        return FluidStack.EMPTY;
    }
}
