package net.unfamily.another_quarries.item;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.registry.ModItems;

public final class QuarryEquipmentSlots {
    private static final int UPGRADE_SLOT_COUNT = 4;

    /** Built-in worker when no drones are installed; slot drones are extra workers on top. */
    public static final int BASE_WORKER_COUNT = 1;

    public static final int MAX_DRILL_STACK = 1;

    public static final int EQUIPMENT_LAYOUT_VERSION = 5;

    /** Layout v3 indices kept for NBT migration only. */
    private static final int LEGACY_V3_DRONE_SLOT = 0;
    private static final int LEGACY_V3_DRILL_SLOT = 1;
    private static final int LEGACY_V3_DIGGER_SLOT = 2;
    private static final int LEGACY_V3_SPEED_SLOT = 3;
    private static final int LEGACY_V3_ENCHANT_SLOT = 4;

    /** @deprecated use {@link #firstDroneSlot()} */
    @Deprecated
    public static final int DRONE_SLOT = 0;

    private QuarryEquipmentSlots() {}

    public static int guiColumnCount() {
        return ModConfig.equipmentGuiColumns();
    }

    public static int droneSlotCount() {
        return ModConfig.equipmentDroneSlots();
    }

    public static int drillSlotCount() {
        return ModConfig.equipmentDrillSlots();
    }

    /** Handler slots: drones + drills + digger/speed/enchant/filter. */
    public static int slotCount() {
        return droneSlotCount() + drillSlotCount() + UPGRADE_SLOT_COUNT;
    }

    public static int firstDroneSlot() {
        return 0;
    }

    public static int drillSlotStart() {
        return droneSlotCount();
    }

    public static int diggerModuleSlot() {
        return drillSlotStart() + drillSlotCount();
    }

    public static int speedModuleSlot() {
        return diggerModuleSlot() + 1;
    }

    public static int enchantModuleSlot() {
        return diggerModuleSlot() + 2;
    }

    public static int filterModuleSlot() {
        return diggerModuleSlot() + 3;
    }

    /** @deprecated use {@link #drillSlotStart()} */
    @Deprecated
    public static int drillSlot() {
        return drillSlotStart();
    }

    public static boolean isDroneSlot(int slot) {
        return slot >= firstDroneSlot() && slot < drillSlotStart();
    }

    public static boolean isDrillSlot(int slot) {
        return slot >= drillSlotStart() && slot < diggerModuleSlot();
    }

    public static boolean isValid(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= slotCount()) {
            return false;
        }
        if (isDroneSlot(slot)) {
            return stack.is(ModItems.DRONE.get());
        }
        if (isDrillSlot(slot)) {
            return ModItems.isAnyDrill(stack);
        }
        if (slot == diggerModuleSlot()) {
            return stack.is(ModItems.MODULE_DIGGER.get());
        }
        if (slot == speedModuleSlot()) {
            return stack.is(ModItems.MODULE_SPEED.get());
        }
        if (slot == enchantModuleSlot()) {
            return stack.is(ModItems.MODULE_FORTUNE.get())
                    || stack.is(ModItems.MODULE_SILK_TOUCH.get())
                    || stack.is(ModItems.MODULE_ENCHANT.get());
        }
        if (slot == filterModuleSlot()) {
            return stack.is(ModItems.MODULE_FILTER.get());
        }
        return false;
    }

    public static int getSlotLimit(int slot, ItemStackHandler handler) {
        if (isDroneSlot(slot)) {
            return ModConfig.maxDrones();
        }
        if (isDrillSlot(slot)) {
            return MAX_DRILL_STACK;
        }
        if (slot == diggerModuleSlot()) {
            return ModConfig.maxDiggerModules();
        }
        if (slot == speedModuleSlot()) {
            return ModConfig.maxSpeedModules();
        }
        if (slot == enchantModuleSlot()) {
            return enchantCap(handler.getStackInSlot(enchantModuleSlot()));
        }
        if (slot == filterModuleSlot()) {
            return ModConfig.maxFilterModules();
        }
        return 0;
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
        if (stack.is(ModItems.MODULE_ENCHANT.get())) {
            return 1;
        }
        return 0;
    }

    public static int droneCount(ItemStackHandler handler) {
        int total = 0;
        for (int slot = firstDroneSlot(); slot < drillSlotStart(); slot++) {
            total += handler.getStackInSlot(slot).getCount();
        }
        return Math.min(total, ModConfig.maxDrones());
    }

    /** One built-in worker plus drones installed across all drone slots (capped by maxDrones). */
    public static int effectiveWorkerCount(ItemStackHandler handler) {
        return BASE_WORKER_COUNT + droneCount(handler);
    }

    public static int diggerModuleCount(ItemStackHandler handler) {
        return handler.getStackInSlot(diggerModuleSlot()).getCount();
    }

    public static int speedModuleCount(ItemStackHandler handler) {
        return handler.getStackInSlot(speedModuleSlot()).getCount();
    }

    public static boolean hasFilterModule(ItemStackHandler handler) {
        return !handler.getStackInSlot(filterModuleSlot()).isEmpty();
    }

    public static int fortuneLevel(ItemStackHandler handler) {
        return fortuneLevel(handler, null);
    }

    public static int fortuneLevel(ItemStackHandler handler, HolderLookup.Provider registries) {
        ItemStack stack = handler.getStackInSlot(enchantModuleSlot());
        if (stack.is(ModItems.MODULE_FORTUNE.get())) {
            return Math.min(stack.getCount(), ModConfig.maxFortuneModules());
        }
        if (stack.is(ModItems.MODULE_ENCHANT.get()) && registries != null) {
            if (hasSilkTouch(handler, registries)) {
                return 0;
            }
            var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
            return Math.min(
                    enchantLevel(stack, lookup.getOrThrow(Enchantments.FORTUNE)),
                    ModConfig.maxFortuneModules());
        }
        return 0;
    }

    public static boolean hasSilkTouch(ItemStackHandler handler) {
        return hasSilkTouch(handler, null);
    }

    public static boolean hasSilkTouch(ItemStackHandler handler, HolderLookup.Provider registries) {
        ItemStack stack = handler.getStackInSlot(enchantModuleSlot());
        if (stack.is(ModItems.MODULE_SILK_TOUCH.get())) {
            return true;
        }
        if (stack.is(ModItems.MODULE_ENCHANT.get()) && registries != null) {
            var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
            return enchantLevel(stack, lookup.getOrThrow(Enchantments.SILK_TOUCH)) > 0;
        }
        return false;
    }

    public static int rawEnchantModuleFortuneLevel(ItemStackHandler handler, HolderLookup.Provider registries) {
        ItemStack stack = handler.getStackInSlot(enchantModuleSlot());
        if (!stack.is(ModItems.MODULE_ENCHANT.get()) || registries == null) {
            return 0;
        }
        var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        return enchantLevel(stack, lookup.getOrThrow(Enchantments.FORTUNE));
    }

    public static boolean rawEnchantModuleSilkTouch(ItemStackHandler handler, HolderLookup.Provider registries) {
        ItemStack stack = handler.getStackInSlot(enchantModuleSlot());
        if (!stack.is(ModItems.MODULE_ENCHANT.get()) || registries == null) {
            return false;
        }
        var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        return enchantLevel(stack, lookup.getOrThrow(Enchantments.SILK_TOUCH)) > 0;
    }

    private static int enchantLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.getEnchantmentLevel(enchantment);
    }

    public static int equipmentMenuIndex(int equipmentSlot) {
        return QuarryBlockEntity.BUFFER_SLOT_COUNT + equipmentSlot;
    }

    public static int legacyV3DroneSlot() {
        return LEGACY_V3_DRONE_SLOT;
    }

    public static int legacyV3DrillSlot() {
        return LEGACY_V3_DRILL_SLOT;
    }

    public static int legacyV3DiggerSlot() {
        return LEGACY_V3_DIGGER_SLOT;
    }

    public static int legacyV3SpeedSlot() {
        return LEGACY_V3_SPEED_SLOT;
    }

    public static int legacyV3EnchantSlot() {
        return LEGACY_V3_ENCHANT_SLOT;
    }
}
