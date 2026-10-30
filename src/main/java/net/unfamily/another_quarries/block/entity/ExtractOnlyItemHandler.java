package net.unfamily.another_quarries.block.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** External automation view: extract only; inserts are rejected. */
public final class ExtractOnlyItemHandler implements IItemHandlerModifiable {
    private final IItemHandlerModifiable delegate;

    public ExtractOnlyItemHandler(IItemHandlerModifiable delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        delegate.setStackInSlot(slot, stack);
    }

    public IItemHandler getDelegate() {
        return delegate;
    }
}
