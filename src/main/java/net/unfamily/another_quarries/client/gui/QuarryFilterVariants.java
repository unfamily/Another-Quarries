package net.unfamily.another_quarries.client.gui;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.unfamily.another_quarries.mining.QuarryFluidFilterMatcher;

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
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return variants;
        }
        variants.add("-" + itemId);
        Item item = stack.getItem();
        Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        holder.tags()
                .map(TagKey::location)
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(tagId -> variants.add("#" + tagId));
        variants.add("@" + itemId.getNamespace());
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

    /**
     * True when a destroy-list line should be edited as a fluid filter
     * (fluid id/tag/mod with no matching item). {@code &anything_else} is pump-only.
     */
    public static boolean isFluidOrientedDestroyLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String trimmed = line.trim();
        if (QuarryFluidFilterMatcher.isAnythingElseLine(trimmed)) {
            return false;
        }
        Fluid fluid = QuarryFilterRowDisplay.resolveFluid(trimmed);
        if (fluid == Fluids.EMPTY) {
            return false;
        }
        return QuarryFilterRowDisplay.previewItemOnly(trimmed).isEmpty();
    }

    /** Pump drain-list variants for a fluid: id, tags, mod prefix and the catch-all macro. */
    public static List<String> variantsFromFluid(Fluid fluid) {
        return variantsFromFluid(fluid, true);
    }

    /** Fluid variants; {@code includeAnythingElse} is true for the pump drain list only. */
    public static List<String> variantsFromFluid(Fluid fluid, boolean includeAnythingElse) {
        List<String> variants = new ArrayList<>();
        if (fluid == null || fluid == Fluids.EMPTY) {
            return variants;
        }
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (fluidId == null) {
            return variants;
        }
        variants.add("-" + fluidId);
        BuiltInRegistries.FLUID.wrapAsHolder(fluid).tags()
                .map(TagKey::location)
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(tagId -> variants.add("#" + tagId));
        variants.add("@" + fluidId.getNamespace());
        if (includeAnythingElse) {
            variants.add(QuarryFluidFilterMatcher.ANYTHING_ELSE);
        }
        return variants;
    }

    /** Fluid contained in an item (bucket, tank), or empty when the stack holds none. */
    public static Fluid fluidContainedIn(ItemStack stack) {
        if (stack.isEmpty()) {
            return Fluids.EMPTY;
        }
        return FluidUtil.getFluidContained(stack)
                .filter(contained -> !contained.isEmpty())
                .map(FluidStack::getFluid)
                .orElse(Fluids.EMPTY);
    }

    public static List<String> variantsForFluidLine(String line) {
        return variantsForFluidLine(line, true);
    }

    public static List<String> variantsForFluidLine(String line, boolean includeAnythingElse) {
        Set<String> out = new LinkedHashSet<>();
        if (line != null && !line.isBlank()) {
            out.addAll(variantsFromFluid(QuarryFilterRowDisplay.resolveFluid(line), includeAnythingElse));
            String trimmed = line.trim();
            if (includeAnythingElse || !QuarryFluidFilterMatcher.isAnythingElseLine(trimmed)) {
                out.add(trimmed);
            }
        }
        return new ArrayList<>(out);
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
