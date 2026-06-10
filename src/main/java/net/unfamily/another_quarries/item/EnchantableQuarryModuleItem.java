package net.unfamily.another_quarries.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.unfamily.another_quarries.registry.ModEnchantmentTags;

/** Single-stack quarry module that stores fortune / silk touch via the enchantment system. */
public final class EnchantableQuarryModuleItem extends DescribedItem {
    public EnchantableQuarryModuleItem(Properties properties, String... tooltipKeys) {
        super(properties, tooltipKeys);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(ModEnchantmentTags.MODULE_ENCHANT_SUPPORTED);
    }
}
