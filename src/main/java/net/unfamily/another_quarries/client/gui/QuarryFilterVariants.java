package net.unfamily.another_quarries.client.gui;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Filter-line variant cycling for quarry destroy-list edit mode. */
public final class QuarryFilterVariants {
    private QuarryFilterVariants() {}

    public static List<String> variantsFromItemStack(ItemStack stack) {
        List<String> variants = new ArrayList<>();
        if (stack.isEmpty()) {
            return variants;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return variants;
        }
        variants.add("-" + itemId);
        variants.add("@" + itemId.getNamespace());
        Item item = stack.getItem();
        Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        holder.tags()
                .map(TagKey::location)
                .map(Identifier::toString)
                .sorted()
                .forEach(tagId -> variants.add("#" + tagId));
        if (stack.isEnchanted() || stack.is(Items.ENCHANTED_BOOK)) {
            variants.add("&enchanted");
        }
        if (stack.isDamageableItem()) {
            if (stack.isDamaged()) {
                variants.add("&damaged");
            }
            variants.add("&damaged>0");
            if (stack.isDamaged()) {
                variants.add("&damaged=" + stack.getDamageValue());
            }
        }
        return variants;
    }

    public static List<String> variantsForLine(String line) {
        Set<String> out = new LinkedHashSet<>();
        ItemStack preview = QuarryFilterRowDisplay.previewStack(line);
        if (!preview.isEmpty()) {
            out.addAll(variantsFromItemStack(preview));
        }
        if (line != null && !line.isBlank()) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        if (out.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(out);
    }

    public static String cycle(List<String> variants, String current, int direction) {
        if (variants == null || variants.isEmpty()) {
            return current != null ? current : "";
        }
        String trimmed = current != null ? current.trim() : "";
        int index = variants.indexOf(trimmed);
        if (index < 0) {
            index = 0;
        } else {
            index += direction;
            if (index < 0) {
                index = variants.size() - 1;
            } else if (index >= variants.size()) {
                index = 0;
            }
        }
        return variants.get(index);
    }

    public static String cycle(String current, int direction) {
        return cycle(variantsForLine(current), current, direction);
    }
}
