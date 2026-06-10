package net.unfamily.another_quarries.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.mining.QuarryItemFilterMatcher;
import net.unfamily.another_quarries.registry.ModDataComponents;
import net.unfamily.another_quarries.registry.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Destroy-list storage on the filter module item stack. */
public final class QuarryFilterModuleData {
    private QuarryFilterModuleData() {}

    public static boolean isFilterModule(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.MODULE_FILTER.get());
    }

    public static List<String> padForEditing(List<String> lines) {
        List<String> out = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                out.add(line != null ? line : "");
            }
        }
        int max = ModConfig.quarryFilterMaxLines();
        while (out.size() < max && (out.isEmpty() || !out.getLast().isEmpty())) {
            out.add("");
        }
        return out;
    }

    public static String draftLine(String raw) {
        return QuarryItemFilterMatcher.sanitizeLine(raw);
    }

    public static List<String> compactDestroyList(List<String> lines) {
        return sanitizeAll(lines);
    }

    public static List<String> getDestroyList(ItemStack stack) {
        if (!isFilterModule(stack)) {
            return List.of();
        }
        List<String> stored = stack.getOrDefault(ModDataComponents.DESTROY_FILTERS.get(), List.of());
        return Collections.unmodifiableList(new ArrayList<>(stored));
    }

    public static List<String> getEditableDestroyList(ItemStack stack) {
        List<String> out = new ArrayList<>(getDestroyList(stack));
        int max = ModConfig.quarryFilterMaxLines();
        while (out.size() < max && (out.isEmpty() || !out.getLast().isEmpty())) {
            out.add("");
        }
        return out;
    }

    public static ItemStack withDestroyList(ItemStack stack, List<String> lines) {
        if (!isFilterModule(stack)) {
            return stack;
        }
        ItemStack copy = stack.copy();
        List<String> sanitized = sanitizeAll(lines);
        if (sanitized.isEmpty()) {
            copy.remove(ModDataComponents.DESTROY_FILTERS.get());
        } else {
            copy.set(ModDataComponents.DESTROY_FILTERS.get(), sanitized);
        }
        return copy;
    }

    public static ItemStack setLine(ItemStack stack, int index, String raw) {
        if (!isFilterModule(stack) || index < 0 || index >= ModConfig.quarryFilterMaxLines()) {
            return stack;
        }
        List<String> lines = new ArrayList<>(getEditableDestroyList(stack));
        while (lines.size() <= index) {
            lines.add("");
        }
        lines.set(index, QuarryItemFilterMatcher.sanitizeLine(raw));
        trimTrailingEmpty(lines);
        return withDestroyList(stack, lines);
    }

    public static void setLineInHand(ServerPlayer player, InteractionHand hand, int index, String raw) {
        ItemStack stack = player.getItemInHand(hand);
        player.setItemInHand(hand, setLine(stack, index, raw));
    }

    public static void replaceAllInHand(ServerPlayer player, InteractionHand hand, List<String> lines) {
        ItemStack stack = player.getItemInHand(hand);
        player.setItemInHand(hand, withDestroyList(stack, lines));
    }

    private static List<String> sanitizeAll(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        int max = ModConfig.quarryFilterMaxLines();
        for (String line : lines) {
            if (out.size() >= max) {
                break;
            }
            String sanitized = QuarryItemFilterMatcher.sanitizeLine(line);
            if (!sanitized.isEmpty()) {
                out.add(sanitized);
            }
        }
        return out;
    }

    private static void trimTrailingEmpty(List<String> lines) {
        while (!lines.isEmpty() && lines.getLast().isEmpty()) {
            lines.removeLast();
        }
    }
}
