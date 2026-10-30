package net.unfamily.another_quarries.item;

import net.minecraft.world.item.ItemStack;
import net.unfamily.another_quarries.config.ModConfig;

/** Single-stack quarry module enchanted via vanilla item tags ({@code #minecraft:enchantable}, {@code #minecraft:enchantable/mining_loot}). */
public final class EnchantableQuarryModuleItem extends DescribedItem {
    public EnchantableQuarryModuleItem(Properties properties, String... tooltipKeys) {
        super(properties, tooltipKeys);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getMaxStackSize() == 1;
    }

    @Override
    public int getEnchantmentValue() {
        return ModConfig.moduleEnchantability();
    }
}
