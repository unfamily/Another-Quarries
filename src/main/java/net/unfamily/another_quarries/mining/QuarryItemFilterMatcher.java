package net.unfamily.another_quarries.mining;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Destroy-list item matching for quarry filter modules ({@code -}, {@code #}, {@code @}, plain id). */
public final class QuarryItemFilterMatcher {
    private QuarryItemFilterMatcher() {}

    public static boolean matchesAny(List<String> filters, ItemStack stack, HolderLookup.Provider registries) {
        if (filters == null || filters.isEmpty() || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        String itemIdStr = itemId.toString();
        String itemModId = itemId.getNamespace();
        for (String raw : filters) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String filter = raw.trim();
            if (filter.isEmpty() || isUnsupportedPrefix(filter)) {
                continue;
            }
            if (matchesFilterEntry(stack, item, itemId, itemIdStr, itemModId, filter, registries)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUnsupportedPrefix(String filter) {
        return filter.startsWith("?") || filter.startsWith("&");
    }

    /** Server-side line validation before persisting. */
    public static String sanitizeLine(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || isUnsupportedPrefix(trimmed)) {
            return "";
        }
        return trimmed;
    }

    public static boolean matchesFilterEntry(
            ItemStack stack,
            Item item,
            Identifier itemId,
            String itemIdStr,
            String itemModId,
            String filter,
            HolderLookup.Provider registries) {
        if (filter.startsWith("-")) {
            return itemIdStr.equals(filter.substring(1));
        }
        if (filter.startsWith("@")) {
            return itemModId.startsWith(filter.substring(1));
        }
        if (filter.startsWith("#")) {
            try {
                Identifier tagId = Identifier.parse(filter.substring(1));
                TagKey<Item> itemTag = ItemTags.create(tagId);
                return item.builtInRegistryHolder().is(itemTag);
            } catch (Exception ignored) {
                return false;
            }
        }
        return itemIdStr.equals(filter);
    }
}
