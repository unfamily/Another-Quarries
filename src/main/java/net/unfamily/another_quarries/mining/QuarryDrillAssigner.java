package net.unfamily.another_quarries.mining;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.item.QuarryEquipmentSlots;
import net.unfamily.another_quarries.registry.ModItems;

public final class QuarryDrillAssigner {
    private QuarryDrillAssigner() {}

    public static QuarryDrillType resolveDrill(ItemStackHandler equipment) {
        ItemStack best = ItemStack.EMPTY;
        for (int slot = QuarryEquipmentSlots.drillSlotStart(); slot < QuarryEquipmentSlots.diggerModuleSlot(); slot++) {
            best = pickBetterDrill(best, equipment.getStackInSlot(slot));
        }
        return drillTypeFromStack(best);
    }

    /** Simulated workers per tick; extra drones are applied as batched breaks instead of more loops. */
    public static int activeWorkerCount(ItemStackHandler equipment) {
        return Math.min(
                QuarryEquipmentSlots.effectiveWorkerCount(equipment),
                ModConfig.maxActiveMiningWorkers());
    }

    private static ItemStack pickBetterDrill(ItemStack current, ItemStack candidate) {
        if (candidate.isEmpty() || !ModItems.isAnyDrill(candidate)) {
            return current;
        }
        if (current.isEmpty()) {
            return candidate.copy();
        }
        return drillTier(candidate) > drillTier(current) ? candidate.copy() : current;
    }

    private static int drillTier(ItemStack stack) {
        if (stack.is(ModItems.DRILL_NETHERITE.get())) {
            return 3;
        }
        if (ModItems.isDrillLaser(stack)) {
            return 4;
        }
        if (stack.is(ModItems.DRILL_DIAMOND.get())) {
            return 2;
        }
        return 1;
    }

    private static QuarryDrillType drillTypeFromStack(ItemStack stack) {
        if (stack.is(ModItems.DRILL_DIAMOND.get())) {
            return QuarryDrillType.DIAMOND;
        }
        if (stack.is(ModItems.DRILL_NETHERITE.get())) {
            return QuarryDrillType.NETHERITE;
        }
        if (ModItems.isDrillLaser(stack)) {
            return QuarryDrillType.DRILL_LASER;
        }
        return QuarryDrillType.BASE;
    }
}
