package net.unfamily.another_quarries.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.unfamily.another_quarries.AnotherQuarries;

public final class ModEnchantmentTags {
    public static final TagKey<Enchantment> MODULE_ENCHANT_SUPPORTED = TagKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "module_enchant_supported"));

    private ModEnchantmentTags() {}
}
