package net.unfamily.another_quarries.item;

import net.minecraft.world.item.Item;
import net.unfamily.another_quarries.config.ModConfig;

/** Single-stack quarry module that stores fortune / silk touch via the enchantment system. */
public final class EnchantableQuarryModuleItem extends DescribedItem {
    public EnchantableQuarryModuleItem(Properties properties, String... tooltipKeys) {
        super(properties, tooltipKeys);
    }

    @Override
    public int getEnchantmentValue() {
        return ModConfig.moduleEnchantability();
    }
}
