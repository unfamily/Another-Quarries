package net.unfamily.another_quarries.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.registry.ModItems;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum QuarryModules {
    BASE(() -> ModItems.MODULE_BASE.get(), "module_base"),
    SPEED(() -> ModItems.MODULE_SPEED.get(), "module_speed"),
    DIGGER(() -> ModItems.MODULE_DIGGER.get(), "module_digger"),
    SILK_TOUCH(() -> ModItems.MODULE_SILK_TOUCH.get(), "module_silktouch"),
    FORTUNE(() -> ModItems.MODULE_FORTUNE.get(), "module_fortune");

    private final DeferredItemRef itemRef;
    private final String configKey;

    QuarryModules(DeferredItemRef itemRef, String configKey) {
        this.itemRef = itemRef;
        this.configKey = configKey;
    }

    public Item getItem() {
        return itemRef.get();
    }

    public String configKey() {
        return configKey;
    }

    public int extraRfPerBlock() {
        return ModConfig.extraRfFor(this);
    }

    public int maxCount() {
        return ModConfig.maxCountFor(this);
    }

    public static boolean isModule(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (QuarryModules module : values()) {
            if (stack.is(module.getItem())) {
                return true;
            }
        }
        return false;
    }

    public static Optional<QuarryModules> fromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        for (QuarryModules module : values()) {
            if (stack.is(module.getItem())) {
                return Optional.of(module);
            }
        }
        return Optional.empty();
    }

    public static List<ItemStack> ghostCycleStacks() {
        return Arrays.stream(values()).map(m -> new ItemStack(m.getItem())).toList();
    }

    @FunctionalInterface
    private interface DeferredItemRef {
        Item get();
    }
}
