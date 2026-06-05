package net.unfamily.another_quarries.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.registry.ModItems;

public final class QuarryEquipmentSlots {
    public static final int DRONE_SLOT = 0;
    public static final int DRILL_SLOT = 1;
    public static final int DIGGER_MODULE_SLOT = 2;
    public static final int SPEED_MODULE_SLOT = 3;
    public static final int ENCHANT_MODULE_SLOT = 4;
    public static final int FILTER_SLOT = 5;
    public static final int SLOT_COUNT = 6;

    /** Built-in worker when no drones are installed; slot drones are extra workers on top. */
    public static final int BASE_WORKER_COUNT = 1;

    public static final int MAX_DRILL_STACK = 1;

    public static final int EQUIPMENT_LAYOUT_VERSION = 3;

    /** @deprecated use {@link #DRILL_SLOT} */
    @Deprecated
    public static final int IRON_DRILL_SLOT = DRILL_SLOT;
    /** @deprecated use {@link #DRILL_SLOT} */
    @Deprecated
    public static final int DIAMOND_DRILL_SLOT = DRILL_SLOT;
    /** @deprecated use {@link #DRILL_SLOT} */
    @Deprecated
    public static final int NETHERITE_DRILL_SLOT = DRILL_SLOT;
    /** @deprecated use {@link #DRILL_SLOT} */
    @Deprecated
    public static final int LASER_DRILL_SLOT = DRILL_SLOT;

    private QuarryEquipmentSlots() {}

    public static boolean isValid(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= SLOT_COUNT) {
            return false;
        }
        return switch (slot) {
            case DRONE_SLOT -> stack.is(ModItems.DRONE.get());
            case DRILL_SLOT -> ModItems.isAnyDrill(stack);
            case DIGGER_MODULE_SLOT -> stack.is(ModItems.MODULE_DIGGER.get());
            case SPEED_MODULE_SLOT -> stack.is(ModItems.MODULE_SPEED.get());
            case ENCHANT_MODULE_SLOT -> stack.is(ModItems.MODULE_FORTUNE.get()) || stack.is(ModItems.MODULE_SILK_TOUCH.get());
            case FILTER_SLOT -> false;
            default -> false;
        };
    }

    public static int getSlotLimit(int slot, ItemStackHandler handler) {
        return switch (slot) {
            case DRONE_SLOT -> ModConfig.maxDrones();
            case DRILL_SLOT -> MAX_DRILL_STACK;
            case DIGGER_MODULE_SLOT -> ModConfig.maxDiggerModules();
            case SPEED_MODULE_SLOT -> ModConfig.maxSpeedModules();
            case ENCHANT_MODULE_SLOT -> enchantCap(handler.getStackInSlot(ENCHANT_MODULE_SLOT));
            case FILTER_SLOT -> 0;
            default -> 0;
        };
    }

    private static int enchantCap(ItemStack stack) {
        if (stack.isEmpty()) {
            return ModConfig.maxFortuneModules();
        }
        if (stack.is(ModItems.MODULE_SILK_TOUCH.get())) {
            return ModConfig.maxSilkTouchModules();
        }
        if (stack.is(ModItems.MODULE_FORTUNE.get())) {
            return ModConfig.maxFortuneModules();
        }
        return 0;
    }

    public static int droneCount(ItemStackHandler handler) {
        return handler.getStackInSlot(DRONE_SLOT).getCount();
    }

    /** One built-in worker plus any drones installed in the drone slot. */
    public static int effectiveWorkerCount(ItemStackHandler handler) {
        return BASE_WORKER_COUNT + droneCount(handler);
    }

    public static int diggerModuleCount(ItemStackHandler handler) {
        return handler.getStackInSlot(DIGGER_MODULE_SLOT).getCount();
    }

    public static int speedModuleCount(ItemStackHandler handler) {
        return handler.getStackInSlot(SPEED_MODULE_SLOT).getCount();
    }

    public static int fortuneLevel(ItemStackHandler handler) {
        ItemStack stack = handler.getStackInSlot(ENCHANT_MODULE_SLOT);
        if (stack.is(ModItems.MODULE_FORTUNE.get())) {
            return Math.min(stack.getCount(), ModConfig.maxFortuneModules());
        }
        return 0;
    }

    public static boolean hasSilkTouch(ItemStackHandler handler) {
        return handler.getStackInSlot(ENCHANT_MODULE_SLOT).is(ModItems.MODULE_SILK_TOUCH.get());
    }

    public static int equipmentMenuIndex(int equipmentSlot) {
        return QuarryBlockEntity.BUFFER_SLOT_COUNT + equipmentSlot;
    }
}
