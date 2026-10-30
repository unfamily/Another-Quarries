package net.unfamily.another_quarries.client.gui;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.another_quarries.mining.QuarryFluidFilterMatcher;
import net.unfamily.another_quarries.mining.QuarryItemFilterMatcher;

import java.util.ArrayList;
import java.util.List;

/** Client preview icon for a single quarry destroy-filter or pump drain-list line. */
public final class QuarryFilterRowDisplay {
    private QuarryFilterRowDisplay() {}

    public static ItemStack previewStack(String filterLine) {
        if (filterLine == null || filterLine.isBlank()) {
            return ItemStack.EMPTY;
        }
        String filter = filterLine.trim();
        if (filter.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // &anything_else is pump-only; ignore on destroy-list preview.
        if (QuarryFluidFilterMatcher.isAnythingElseLine(filter)
                || (QuarryItemFilterMatcher.isUnsupportedPrefix(filter) && !filter.startsWith("&"))) {
            return ItemStack.EMPTY;
        }
        ItemStack item = previewItemOnly(filter);
        if (!item.isEmpty()) {
            return item;
        }
        // Destroy lists also void fluids: fall back to the fluid bucket.
        return bucketFor(resolveFluid(filter));
    }

    /** Item-side preview only (no fluid-bucket fallback). */
    public static ItemStack previewItemOnly(String filterLine) {
        if (filterLine == null || filterLine.isBlank()) {
            return ItemStack.EMPTY;
        }
        return previewItemOnlyTrimmed(filterLine.trim());
    }

    /** Preview for the pump drain list: fluids are shown as their bucket item. */
    public static ItemStack previewStackForPump(String filterLine) {
        if (filterLine == null || filterLine.isBlank()) {
            return ItemStack.EMPTY;
        }
        String filter = filterLine.trim();
        if (QuarryFluidFilterMatcher.isAnythingElseLine(filter)) {
            return new ItemStack(Items.KNOWLEDGE_BOOK);
        }
        Fluid fluid = resolveFluid(filter);
        return bucketFor(fluid);
    }

    /** Resolves the first fluid referenced by a drain-list line (id, tag or mod prefix). */
    public static Fluid resolveFluid(String filter) {
        if (filter == null || filter.isBlank()) {
            return Fluids.EMPTY;
        }
        String f = filter.trim();
        if (f.startsWith("-")) {
            return fluidById(f.substring(1));
        }
        if (f.startsWith("#")) {
            return fluidForTag(f.substring(1));
        }
        if (f.startsWith("@")) {
            return fluidForMod(f.substring(1));
        }
        if (f.startsWith("&") || f.startsWith("?")) {
            return Fluids.EMPTY;
        }
        return fluidById(f);
    }

    public static ItemStack bucketFor(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return ItemStack.EMPTY;
        }
        Item bucket = fluid.getBucket();
        if (bucket == null || bucket == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(bucket);
    }

    /** FluidStack for slot rendering (1000 mB), or empty when the line is not a fluid filter. */
    public static FluidStack previewFluidStack(String filterLine) {
        Fluid fluid = resolveFluid(filterLine);
        if (fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, 1000);
    }

    private static ItemStack previewItemOnlyTrimmed(String filter) {
        if (filter.startsWith("-")) {
            return itemByIdOnly(filter.substring(1));
        }
        if (filter.startsWith("#")) {
            return itemForTag(filter.substring(1));
        }
        if (filter.startsWith("@")) {
            return itemForMod(filter.substring(1));
        }
        if (filter.startsWith("&") || filter.startsWith("?")) {
            return ItemStack.EMPTY;
        }
        return itemByIdOnly(filter);
    }

    private static Fluid fluidById(String idFilter) {
        try {
            Identifier id = Identifier.parse(idFilter);
            return BuiltInRegistries.FLUID.get(id)
                    .map(holder -> holder.value())
                    .orElse(Fluids.EMPTY);
        } catch (Exception ignored) {
            return Fluids.EMPTY;
        }
    }

    private static Fluid fluidForTag(String tagId) {
        try {
            TagKey<Fluid> fluidTag = TagKey.create(Registries.FLUID, Identifier.parse(tagId));
            List<Fluid> fluids = new ArrayList<>();
            for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(fluidTag)) {
                if (!bucketFor(holder.value()).isEmpty()) {
                    fluids.add(holder.value());
                }
            }
            if (fluids.isEmpty()) {
                return Fluids.EMPTY;
            }
            return fluids.get((int) ((System.currentTimeMillis() / 2000L) % fluids.size()));
        } catch (Exception ignored) {
            return Fluids.EMPTY;
        }
    }

    private static Fluid fluidForMod(String modPrefix) {
        if (modPrefix.isEmpty()) {
            return Fluids.EMPTY;
        }
        List<Fluid> fluids = new ArrayList<>();
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
            if (id != null && id.getNamespace().startsWith(modPrefix) && !bucketFor(fluid).isEmpty()) {
                fluids.add(fluid);
            }
        }
        if (fluids.isEmpty()) {
            return Fluids.EMPTY;
        }
        return fluids.get((int) ((System.currentTimeMillis() / 2000L) % fluids.size()));
    }

    private static ItemStack itemByIdOnly(String idFilter) {
        try {
            Identifier id = Identifier.parse(idFilter);
            return BuiltInRegistries.ITEM.get(id)
                    .map(holder -> new ItemStack(holder.value()))
                    .orElse(ItemStack.EMPTY);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack itemForTag(String tagId) {
        try {
            TagKey<Item> itemTag = ItemTags.create(Identifier.parse(tagId));
            var holders = BuiltInRegistries.ITEM.getTagOrEmpty(itemTag);
            int count = 0;
            for (Holder<Item> ignored : holders) {
                count++;
            }
            if (count == 0) {
                return ItemStack.EMPTY;
            }
            int pick = (int) ((System.currentTimeMillis() / 2000L) % count);
            int i = 0;
            for (Holder<Item> holder : holders) {
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
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && id.getNamespace().startsWith(modPrefix)) {
                modItems.add(item);
            }
        }
        if (modItems.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(modItems.get((int) ((System.currentTimeMillis() / 2000L) % modItems.size())));
    }
}
