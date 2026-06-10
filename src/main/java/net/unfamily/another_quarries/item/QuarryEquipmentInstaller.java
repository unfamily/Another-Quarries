package net.unfamily.another_quarries.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.registry.ModItems;

/** Installs quarry equipment from the player's hand without opening the quarry GUI. */
public final class QuarryEquipmentInstaller {
    private QuarryEquipmentInstaller() {}

    public static boolean tryInstallFromHand(QuarryBlockEntity quarry, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || !ModItems.isQuarryEquipment(held)) {
            return false;
        }
        ItemStack remaining = installIntoHandler(quarry.getEquipmentHandler(), held);
        if (remaining.getCount() == held.getCount()) {
            return false;
        }
        player.setItemInHand(hand, remaining);
        quarry.setChanged();
        return true;
    }

    private static ItemStack installIntoHandler(ItemStackHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();
        if (ModItems.isDrone(remaining)) {
            remaining = insertIntoSlotRange(handler, remaining,
                    QuarryEquipmentSlots.firstDroneSlot(), QuarryEquipmentSlots.drillSlotStart());
        } else if (ModItems.isAnyDrill(remaining)) {
            remaining = insertIntoSlotRange(handler, remaining,
                    QuarryEquipmentSlots.drillSlotStart(), QuarryEquipmentSlots.diggerModuleSlot());
        } else if (remaining.is(ModItems.MODULE_DIGGER.get())) {
            remaining = insertIntoSlot(handler, remaining, QuarryEquipmentSlots.diggerModuleSlot());
        } else if (remaining.is(ModItems.MODULE_SPEED.get())) {
            remaining = insertIntoSlot(handler, remaining, QuarryEquipmentSlots.speedModuleSlot());
        } else if (remaining.is(ModItems.MODULE_FORTUNE.get())
                || remaining.is(ModItems.MODULE_SILK_TOUCH.get())
                || remaining.is(ModItems.MODULE_ENCHANT.get())) {
            remaining = insertIntoSlot(handler, remaining, QuarryEquipmentSlots.enchantModuleSlot());
        } else if (remaining.is(ModItems.MODULE_FILTER.get())) {
            remaining = insertIntoSlot(handler, remaining, QuarryEquipmentSlots.filterModuleSlot());
        }
        return remaining;
    }

    private static ItemStack insertIntoSlotRange(ItemStackHandler handler, ItemStack stack, int start, int end) {
        ItemStack remaining = stack.copy();
        for (int slot = start; slot < end && !remaining.isEmpty(); slot++) {
            remaining = insertIntoSlot(handler, remaining, slot);
        }
        return remaining;
    }

    private static ItemStack insertIntoSlot(ItemStackHandler handler, ItemStack stack, int slot) {
        if (stack.isEmpty() || slot < 0 || slot >= handler.getSlots()) {
            return stack;
        }
        ItemStack inSlot = handler.getStackInSlot(slot);
        if (!inSlot.isEmpty() && !ItemStack.isSameItemSameComponents(inSlot, stack)) {
            return stack;
        }
        if (!QuarryEquipmentSlots.isValid(slot, stack)) {
            return stack;
        }
        return handler.insertItem(slot, stack, false);
    }
}
