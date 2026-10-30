package net.unfamily.another_quarries.item;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.mining.QuarryFluidFilterMatcher;
import net.unfamily.another_quarries.mining.QuarryItemFilterMatcher;
import net.unfamily.another_quarries.registry.ModDataComponents;
import net.unfamily.another_quarries.registry.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Line-list storage on quarry list modules: the destroy list on the filter module and the drain list on the
 * pump module. Both share the same editor GUI; the stack decides the component and sanitization rules.
 */
public final class QuarryFilterModuleData {
    private QuarryFilterModuleData() {}

    public static boolean isFilterModule(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.MODULE_FILTER.get());
    }

    public static boolean isPumpModule(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.MODULE_PUMP.get());
    }

    /** True for stacks that carry an editable line list (filter or pump module). */
    public static boolean isListModule(ItemStack stack) {
        return isFilterModule(stack) || isPumpModule(stack);
    }

    private static DataComponentType<List<String>> componentFor(ItemStack stack) {
        return isPumpModule(stack) ? ModDataComponents.DRAIN_FILTERS.get() : ModDataComponents.DESTROY_FILTERS.get();
    }

    private static String sanitizeFor(ItemStack stack, String raw) {
        return isPumpModule(stack)
                ? QuarryFluidFilterMatcher.sanitizeLine(raw)
                : sanitizeDestroyLine(raw);
    }

    /**
     * Destroy-list sanitizer: accepts item lines and fluid id/tag/mod lines.
     * {@code &anything_else} has no meaning on the destroy list (pump drain only).
     */
    public static String sanitizeDestroyLine(String raw) {
        if (QuarryFluidFilterMatcher.isAnythingElseLine(raw)) {
            return "";
        }
        String fluid = QuarryFluidFilterMatcher.sanitizeLine(raw);
        if (!fluid.isEmpty()) {
            return fluid;
        }
        return QuarryItemFilterMatcher.sanitizeLine(raw);
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

    /** Sanitizes one draft line for the given list module (filter: item+fluid rules, pump: fluid rules). */
    public static String draftLine(boolean pump, String raw) {
        return pump ? QuarryFluidFilterMatcher.sanitizeLine(raw) : sanitizeDestroyLine(raw);
    }

    public static String draftLine(String raw) {
        return sanitizeDestroyLine(raw);
    }

    public static List<String> compactDestroyList(List<String> lines) {
        return sanitizeAll(false, lines);
    }

    public static List<String> compactList(boolean pump, List<String> lines) {
        return sanitizeAll(pump, lines);
    }

    /** Destroy list (filter module) or drain list (pump module). */
    public static List<String> getDestroyList(ItemStack stack) {
        if (!isListModule(stack)) {
            return List.of();
        }
        List<String> stored = stack.getOrDefault(componentFor(stack), List.of());
        return Collections.unmodifiableList(new ArrayList<>(stored));
    }

    public static List<String> getDrainList(ItemStack stack) {
        return isPumpModule(stack) ? getDestroyList(stack) : List.of();
    }

    /** True when the module stack has at least one non-blank configured filter/drain line. */
    public static boolean hasConfiguredLines(ItemStack stack) {
        for (String line : getDestroyList(stack)) {
            if (line != null && !line.isBlank()) {
                return true;
            }
        }
        return false;
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
        if (!isListModule(stack)) {
            return stack;
        }
        ItemStack copy = stack.copy();
        List<String> sanitized = sanitizeAll(isPumpModule(stack), lines);
        if (sanitized.isEmpty()) {
            copy.remove(componentFor(stack));
        } else {
            copy.set(componentFor(stack), sanitized);
        }
        return copy;
    }

    public static ItemStack setLine(ItemStack stack, int index, String raw) {
        if (!isListModule(stack) || index < 0 || index >= ModConfig.quarryFilterMaxLines()) {
            return stack;
        }
        List<String> lines = new ArrayList<>(getEditableDestroyList(stack));
        while (lines.size() <= index) {
            lines.add("");
        }
        lines.set(index, sanitizeFor(stack, raw));
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

    private static List<String> sanitizeAll(boolean pump, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        int max = ModConfig.quarryFilterMaxLines();
        for (String line : lines) {
            if (out.size() >= max) {
                break;
            }
            String sanitized = draftLine(pump, line);
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
