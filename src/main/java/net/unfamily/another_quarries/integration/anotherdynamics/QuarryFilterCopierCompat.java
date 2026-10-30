package net.unfamily.another_quarries.integration.anotherdynamics;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.item.QuarryFilterModuleData;
import net.unfamily.another_quarries.mining.QuarryFluidFilterMatcher;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Optional Settings Copier FILTER list snapshot import/export compatible with Another Dynamics.
 * External component ids: {@code another_dynamics:duct_face_settings}, {@code another_dynamics:settings_copier_filter}.
 *
 * <p>Like AD duct/sequential GUIs, copy/paste operate on the Settings Copier placed in the GUI copy slot
 * — not a phantom inventory fallback. Quarry filter copies as {@code MaterialKind.ITEM} and pastes ITEM or FLUID;
 * pump copies/pastes as {@code MaterialKind.FLUID} only.</p>
 */
public final class QuarryFilterCopierCompat {
    private static final String AD_MOD_ID = "another_dynamics";
    /** Maps to {@code another_dynamics:duct_face_settings} */
    private static final ResourceLocation AD_SETTINGS_SNAPSHOT_COMPONENT =
            ResourceLocation.fromNamespaceAndPath(AD_MOD_ID, "duct_face_settings");
    /** Maps to {@code another_dynamics:settings_copier_filter} */
    private static final ResourceLocation AD_COPIER_FILTER_MODE_COMPONENT =
            ResourceLocation.fromNamespaceAndPath(AD_MOD_ID, "settings_copier_filter");
    private static final ResourceLocation SETTINGS_COPIER_ITEM_ID =
            ResourceLocation.fromNamespaceAndPath(AD_MOD_ID, "settings_copier");

    private static final String KEY_FMT = "Fmt";
    private static final String KEY_KIND = "Kind";
    private static final String KEY_MATERIAL_KIND = "MaterialKind";
    private static final String KEY_LINES = "Lines";

    private static final int FORMAT_VERSION = 3;
    /** {@code SettingsCopierStoreKind.FILTER} */
    private static final byte STORE_KIND_FILTER = 1;
    /** {@code FilterListMaterialKind.ITEM} */
    public static final byte MATERIAL_KIND_ITEM = 1;
    /** {@code FilterListMaterialKind.FLUID} */
    public static final byte MATERIAL_KIND_FLUID = 2;
    /** {@code FilterListMaterialKind.NONE} / missing */
    public static final byte MATERIAL_KIND_NONE = 0;

    private QuarryFilterCopierCompat() {}

    /**
     * Whether {@code copier} (GUI copy-slot stack) can be pasted into the given list module
     * (filter: ITEM or FLUID; pump: FLUID only).
     */
    public static boolean isCopierCompatibleWithModule(ItemStack copier, ItemStack listModule) {
        if (copier == null || copier.isEmpty() || !QuarryFilterModuleData.isListModule(listModule)) {
            return false;
        }
        if (!isCopierFilterListMode(copier)) {
            return false;
        }
        byte kind = getMaterialKind(copier);
        if (QuarryFilterModuleData.isPumpModule(listModule)) {
            return kind == MATERIAL_KIND_FLUID;
        }
        return kind == MATERIAL_KIND_ITEM || kind == MATERIAL_KIND_FLUID;
    }

    /** MaterialKind from a Settings Copier snapshot, or {@link #MATERIAL_KIND_NONE}. */
    public static byte getMaterialKind(ItemStack copier) {
        if (copier == null || copier.isEmpty()) {
            return MATERIAL_KIND_NONE;
        }
        DataComponentType<CompoundTag> settingsType = settingsSnapshotComponentType();
        if (settingsType == null || !copier.has(settingsType)) {
            return MATERIAL_KIND_NONE;
        }
        return materialKindOf(copier.get(settingsType));
    }

    public static CopyResult copyLinesToCopier(
            ItemStack copier,
            List<String> lines,
            byte materialKind,
            UnaryOperator<String> sanitizer,
            String successKey) {
        if (!AnotherDynamicsIntegration.isLoaded()) {
            return CopyResult.failure("message.another_quarries.quarry.filter.copier.ad_missing");
        }
        if (copier == null || copier.isEmpty() || !isSettingsCopierItem(copier)) {
            return CopyResult.failure("message.another_quarries.quarry.filter.copier.missing");
        }
        DataComponentType<CompoundTag> settingsType = settingsSnapshotComponentType();
        if (settingsType == null) {
            return CopyResult.failure("message.another_quarries.quarry.filter.copier.ad_missing");
        }
        CompoundTag snapshot = buildFilterListSnapshot(lines, materialKind, sanitizer);
        copier.set(settingsType, snapshot);
        DataComponentType<Boolean> filterFlag = copierFilterModeComponentType();
        if (filterFlag != null) {
            copier.set(filterFlag, true);
        }
        return CopyResult.success(successKey);
    }

    public static PasteResult readLinesFromCopier(ItemStack copier, boolean pump) {
        if (!AnotherDynamicsIntegration.isLoaded()) {
            return PasteResult.failure("message.another_quarries.quarry.filter.copier.ad_missing");
        }
        if (copier == null || copier.isEmpty() || !isSettingsCopierItem(copier)) {
            return PasteResult.failure("message.another_quarries.quarry.filter.copier.missing");
        }
        if (!isCopierFilterListMode(copier)) {
            return PasteResult.failure("message.another_quarries.quarry.filter.copier.not_filter_mode");
        }
        DataComponentType<CompoundTag> settingsType = settingsSnapshotComponentType();
        if (settingsType == null || !copier.has(settingsType)) {
            return PasteResult.failure("message.another_quarries.quarry.filter.copier.empty");
        }
        CompoundTag snapshot = copier.get(settingsType);
        if (!isFilterListSnapshot(snapshot)) {
            return PasteResult.failure("message.another_quarries.quarry.filter.copier.not_filter");
        }
        byte kind = materialKindOf(snapshot);
        if (pump) {
            if (kind != MATERIAL_KIND_FLUID) {
                return PasteResult.failure("message.another_quarries.quarry.pump.copier.not_fluid");
            }
        } else if (kind != MATERIAL_KIND_ITEM && kind != MATERIAL_KIND_FLUID) {
            return PasteResult.failure("message.another_quarries.quarry.filter.copier.not_filter");
        }
        UnaryOperator<String> sanitizer = pump
                ? QuarryFluidFilterMatcher::sanitizeLine
                : QuarryFilterModuleData::sanitizeDestroyLine;
        List<String> imported = sanitizeImported(readLines(snapshot), sanitizer);
        String successKey = pump
                ? "message.another_quarries.quarry.pump.copier.pasted"
                : "message.another_quarries.quarry.filter.copier.pasted";
        return PasteResult.success(successKey, imported.size(), imported);
    }

    public static boolean isSettingsCopierItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return SETTINGS_COPIER_ITEM_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** True when copier is in single filter-list mode (not full node ALL mode). */
    public static boolean isCopierFilterListMode(ItemStack copier) {
        if (copier == null || copier.isEmpty()) {
            return false;
        }
        DataComponentType<Boolean> filterFlag = copierFilterModeComponentType();
        if (filterFlag != null && copier.has(filterFlag)) {
            return Boolean.TRUE.equals(copier.get(filterFlag));
        }
        DataComponentType<CompoundTag> settingsType = settingsSnapshotComponentType();
        if (settingsType == null || !copier.has(settingsType)) {
            return false;
        }
        CompoundTag tag = copier.get(settingsType);
        return tag != null
                && tag.contains(KEY_KIND)
                && tag.getByte(KEY_KIND) == STORE_KIND_FILTER;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable DataComponentType<CompoundTag> settingsSnapshotComponentType() {
        if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(AD_SETTINGS_SNAPSHOT_COMPONENT)) {
            return null;
        }
        return (DataComponentType<CompoundTag>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(AD_SETTINGS_SNAPSHOT_COMPONENT);
    }

    @SuppressWarnings("unchecked")
    private static @Nullable DataComponentType<Boolean> copierFilterModeComponentType() {
        if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(AD_COPIER_FILTER_MODE_COMPONENT)) {
            return null;
        }
        return (DataComponentType<Boolean>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(AD_COPIER_FILTER_MODE_COMPONENT);
    }

    private static CompoundTag buildFilterListSnapshot(
            List<String> lines, byte materialKind, UnaryOperator<String> sanitizer) {
        List<String> safe = sanitizeImported(lines, sanitizer);
        CompoundTag root = new CompoundTag();
        root.putInt(KEY_FMT, FORMAT_VERSION);
        root.putByte(KEY_KIND, STORE_KIND_FILTER);
        root.putByte(KEY_MATERIAL_KIND, materialKind);
        ListTag list = new ListTag();
        for (String line : safe) {
            list.add(StringTag.valueOf(line));
        }
        root.put(KEY_LINES, list);
        return root;
    }

    private static List<String> sanitizeImported(List<String> lines, UnaryOperator<String> sanitizer) {
        List<String> safe = new ArrayList<>();
        int max = ModConfig.quarryFilterMaxLines();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String sanitized = sanitizer.apply(line);
            if (!sanitized.isEmpty()) {
                safe.add(sanitized);
            }
            if (safe.size() >= max) {
                break;
            }
        }
        return safe;
    }

    private static boolean isFilterListSnapshot(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains(KEY_FMT)) {
            return false;
        }
        int fmt = tag.getInt(KEY_FMT);
        if (fmt != FORMAT_VERSION && fmt != 2) {
            return false;
        }
        return tag.contains(KEY_KIND) && tag.getByte(KEY_KIND) == STORE_KIND_FILTER;
    }

    private static byte materialKindOf(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains(KEY_MATERIAL_KIND)) {
            // Legacy snapshots without MaterialKind are treated as ITEM.
            return MATERIAL_KIND_ITEM;
        }
        return tag.getByte(KEY_MATERIAL_KIND);
    }

    private static List<String> readLines(CompoundTag tag) {
        List<String> out = new ArrayList<>();
        if (!tag.contains(KEY_LINES)) {
            return out;
        }
        ListTag list = tag.getList(KEY_LINES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }

    public record CopyResult(boolean success, String messageKey) {
        public static CopyResult success(String key) {
            return new CopyResult(true, key);
        }

        public static CopyResult failure(String key) {
            return new CopyResult(false, key);
        }

        public void sendTo(ServerPlayer player) {
            player.sendSystemMessage(Component.translatable(messageKey), true);
        }
    }

    public record PasteResult(boolean success, String messageKey, int lineCount, List<String> lines) {
        public static PasteResult success(String key, int lines, List<String> imported) {
            return new PasteResult(true, key, lines, List.copyOf(imported));
        }

        public static PasteResult failure(String key) {
            return new PasteResult(false, key, 0, List.of());
        }

        public void sendTo(ServerPlayer player) {
            if (success) {
                player.sendSystemMessage(Component.translatable(messageKey, lineCount), true);
            } else {
                player.sendSystemMessage(Component.translatable(messageKey), true);
            }
        }
    }
}
