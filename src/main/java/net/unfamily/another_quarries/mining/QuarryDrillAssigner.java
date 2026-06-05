package net.unfamily.another_quarries.mining;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.another_quarries.item.QuarryEquipmentSlots;
import net.unfamily.another_quarries.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

public final class QuarryDrillAssigner {
    private QuarryDrillAssigner() {}

    public static QuarryDrillType resolveDrill(ItemStackHandler equipment) {
        ItemStack stack = equipment.getStackInSlot(QuarryEquipmentSlots.DRILL_SLOT);
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

    public static List<QuarryDrillType> assign(ItemStackHandler equipment) {
        int workers = QuarryEquipmentSlots.effectiveWorkerCount(equipment);
        QuarryDrillType drill = resolveDrill(equipment);
        List<QuarryDrillType> assigned = new ArrayList<>(workers);
        for (int i = 0; i < workers; i++) {
            assigned.add(drill);
        }
        return assigned;
    }

    public static int totalAssignedDrills(List<QuarryDrillType> assigned) {
        return assigned.size();
    }
}
