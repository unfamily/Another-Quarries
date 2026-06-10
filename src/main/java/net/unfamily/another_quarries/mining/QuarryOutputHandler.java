package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public final class QuarryOutputHandler {
    private QuarryOutputHandler() {}

    public static boolean hasBufferSpace(ItemStackHandler buffer) {
        for (int i = 0; i < buffer.getSlots(); i++) {
            ItemStack stack = buffer.getStackInSlot(i);
            if (stack.isEmpty()) {
                return true;
            }
            if (stack.getCount() < buffer.getSlotLimit(i)) {
                return true;
            }
        }
        return false;
    }

    /** Voids buffer stacks matching the installed filter module destroy list (same rules as {@link QuarryBlockBreaker}). */
    public static void purgeFilteredItemsFromBuffer(
            ItemStackHandler buffer,
            List<String> destroyFilters,
            HolderLookup.Provider registries) {
        if (destroyFilters == null || destroyFilters.isEmpty() || registries == null) {
            return;
        }
        for (int slot = 0; slot < buffer.getSlots(); slot++) {
            ItemStack stack = buffer.getStackInSlot(slot);
            if (!stack.isEmpty() && QuarryItemFilterMatcher.matchesAny(destroyFilters, stack, registries)) {
                buffer.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    public static void tryEjectBufferUp(ServerLevel level, BlockPos quarryPos, ItemStackHandler buffer) {
        BlockPos above = quarryPos.above();
        IItemHandler destination = level.getCapability(
                Capabilities.ItemHandler.BLOCK, above, Direction.DOWN);
        if (destination == null) {
            return;
        }
        if (!canAcceptAny(buffer, destination)) {
            return;
        }
        for (int slot = 0; slot < buffer.getSlots(); slot++) {
            ItemStack stack = buffer.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(destination, stack, false);
            if (remainder.getCount() != stack.getCount()) {
                buffer.setStackInSlot(slot, remainder);
            }
        }
    }

    private static boolean canAcceptAny(IItemHandler source, IItemHandler destination) {
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(destination, stack.copy(), true);
            if (remainder.getCount() < stack.getCount()) {
                return true;
            }
        }
        return false;
    }
}
