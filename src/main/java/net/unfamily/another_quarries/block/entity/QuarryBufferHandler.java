package net.unfamily.another_quarries.block.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Quarry item buffer. GUI may insert/extract; pipes use an extract-only capability wrap. */
public class QuarryBufferHandler extends ItemStackHandler {
    public QuarryBufferHandler(int size) {
        super(size);
    }

    /** Inserts drops from quarry mining across free slots. */
    public void insertMinedItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remaining = stack.copy();
        for (int i = 0; i < getSlots(); i++) {
            remaining = insertItem(i, remaining, false);
            if (remaining.isEmpty()) {
                return;
            }
        }
    }
}
