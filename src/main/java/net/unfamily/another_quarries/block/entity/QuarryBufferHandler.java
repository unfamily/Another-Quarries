package net.unfamily.another_quarries.block.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Quarry output buffer: extract-only for pipes/GUI; mining code uses {@link #insertMinedItem}. */
public class QuarryBufferHandler extends ItemStackHandler {
    public QuarryBufferHandler(int size) {
        super(size);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    /** Inserts drops from quarry mining; bypasses the external insert lock above. */
    public void insertMinedItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remaining = stack.copy();
        for (int i = 0; i < getSlots(); i++) {
            remaining = super.insertItem(i, remaining, false);
            if (remaining.isEmpty()) {
                return;
            }
        }
    }
}
