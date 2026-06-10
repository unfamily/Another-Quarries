package net.unfamily.another_quarries.client.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unfamily.another_quarries.mining.QuarryItemFilterMatcher;

import java.util.ArrayList;
import java.util.List;

/** Client preview icon for a single quarry deny-filter line. */
public final class QuarryFilterRowDisplay {
    private QuarryFilterRowDisplay() {}

    public static ItemStack previewStack(String filterLine) {
        if (filterLine == null || filterLine.isBlank()) {
            return ItemStack.EMPTY;
        }
        String filter = filterLine.trim();
        if (filter.isEmpty() || QuarryItemFilterMatcher.isUnsupportedPrefix(filter)) {
            return ItemStack.EMPTY;
        }
        if (filter.startsWith("-")) {
            return itemById(filter.substring(1));
        }
        if (filter.startsWith("#")) {
            return itemForTag(filter.substring(1));
        }
        if (filter.startsWith("@")) {
            return itemForMod(filter.substring(1));
        }
        return itemById(filter);
    }

    private static ItemStack itemById(String idFilter) {
        try {
            ResourceLocation id = ResourceLocation.parse(idFilter);
            Item item = BuiltInRegistries.ITEM.get(id);
            return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack itemForTag(String tagId) {
        try {
            TagKey<Item> itemTag = ItemTags.create(ResourceLocation.parse(tagId));
            var holders = BuiltInRegistries.ITEM.getTagOrEmpty(itemTag);
            int count = 0;
            for (var ignored : holders) {
                count++;
            }
            if (count == 0) {
                return ItemStack.EMPTY;
            }
            int pick = (int) ((System.currentTimeMillis() / 2000L) % count);
            int i = 0;
            for (var holder : holders) {
                if (i == pick) {
                    return new ItemStack(holder.value());
                }
                i++;
            }
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack itemForMod(String modPrefix) {
        if (modPrefix.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<Item> modItems = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && id.getNamespace().startsWith(modPrefix)) {
                modItems.add(item);
            }
        }
        if (modItems.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int index = (int) ((System.currentTimeMillis() / 2000L) % modItems.size());
        return new ItemStack(modItems.get(index));
    }
}
