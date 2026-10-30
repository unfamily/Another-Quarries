package net.unfamily.another_quarries.mining;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Fluid list matching for the quarry pump drain list and the filter destroy list.
 * Supports {@code -id}, {@code #namespace:tag}, {@code @mod}, plain registry id and {@code &anything_else}
 * (adapted from the Another Dynamics duct fluid filter matcher).
 */
public final class QuarryFluidFilterMatcher {
    /** Matches any non-empty fluid that reaches this line. */
    public static final String ANYTHING_ELSE = "&anything_else";

    private static final String ANYTHING_ELSE_BODY = "anything_else";

    private QuarryFluidFilterMatcher() {}

    public static boolean isAnythingElseLine(@Nullable String filter) {
        if (filter == null) {
            return false;
        }
        String t = filter.trim();
        if (t.equalsIgnoreCase(ANYTHING_ELSE) || t.equalsIgnoreCase(ANYTHING_ELSE_BODY)) {
            return true;
        }
        return t.startsWith("&") && t.substring(1).trim().equalsIgnoreCase(ANYTHING_ELSE_BODY);
    }

    public static boolean matchesAny(List<String> filters, Fluid fluid) {
        if (filters == null || filters.isEmpty() || fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        Identifier fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        String fluidIdStr = fluidId.toString();
        String fluidModId = fluidId.getNamespace();
        for (String raw : filters) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String filter = raw.trim();
            if (matchesFilterEntry(fluid, fluidIdStr, fluidModId, filter)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesFilterEntry(Fluid fluid, String fluidIdStr, String fluidModId, String filter) {
        if (filter == null || filter.isEmpty()) {
            return false;
        }
        if (filter.startsWith("-")) {
            return fluidIdStr.equals(filter.substring(1));
        }
        if (filter.startsWith("&")) {
            return isAnythingElseLine(filter);
        }
        if (filter.startsWith("?")) {
            return false;
        }
        if (filter.startsWith("@")) {
            return fluidModId.startsWith(filter.substring(1));
        }
        if (filter.startsWith("#")) {
            try {
                Identifier tagId = Identifier.parse(filter.substring(1));
                TagKey<Fluid> fluidTag = TagKey.create(Registries.FLUID, tagId);
                return BuiltInRegistries.FLUID.wrapAsHolder(fluid).is(fluidTag);
            } catch (Exception ignored) {
                return false;
            }
        }
        if (isAnythingElseLine(filter)) {
            return true;
        }
        return fluidIdStr.equals(filter);
    }

    /** Server-side drain-list line validation before persisting. */
    public static String sanitizeLine(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("?")) {
            return "";
        }
        if (isAnythingElseLine(trimmed)) {
            return ANYTHING_ELSE;
        }
        if (trimmed.startsWith("&")) {
            return "";
        }
        return trimmed;
    }
}
