package net.unfamily.another_quarries.integration.anotherdynamics;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.item.QuarryFilterModuleData;
import net.unfamily.another_quarries.mining.QuarryItemFilterMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional Settings Copier FILTER list snapshot import/export compatible with Another Dynamics.
 * External component ids: {@code another_dynamics:duct_face_settings}, {@code another_dynamics:settings_copier_filter}.
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
    private static final byte MATERIAL_KIND_ITEM = 1;

    private QuarryFilterCopierCompat() {}

    public static boolean playerHoldsSettingsCopier(Player player) {
        return findSettingsCopier(player) != null;
    }

    public static boolean isHeldCopierFilterListMode(Player player) {
        ItemStack copier = findSettingsCopier(player);
        return copier != null && isCopierFilterListMode(copier);
    }

    public static CopyResult copyToCopier(ServerPlayer player, InteractionHand hand) {
        if (!AnotherDynamicsIntegration.isLoaded()) {
            return CopyResult.failure("message.another_quarries.quarry.filter.copier.ad_missing");
        }
        ItemStack filterModule = player.getItemInHand(hand);
        if (!QuarryFilterModuleData.isFilterModule(filterModule)) {
            return CopyResult.failure("message.another_quarries.quarry.filter.copier.missing");
        }
        ItemStack copier = findSettingsCopier(player);
        if (copier == null) {
            return CopyResult.failure("message.another_quarries.quarry.filter.copier.missing");
        }
        DataComponentType<CompoundTag> settingsType = settingsSnapshotComponentType();
        if (settingsType == null) {
            return CopyResult.failure("message.another_quarries.quarry.filter.copier.ad_missing");
        }
        List<String> lines = QuarryFilterModuleData.getDestroyList(filterModule);
        CompoundTag snapshot = buildFilterListSnapshot(lines);
        copier.set(settingsType, snapshot);
        DataComponentType<Boolean> filterFlag = copierFilterModeComponentType();
        if (filterFlag != null) {
            copier.set(filterFlag, true);
        }
        return CopyResult.success("message.another_quarries.quarry.filter.copier.copied");
    }

    public static PasteResult pasteFromCopier(ServerPlayer player, InteractionHand hand) {
        if (!AnotherDynamicsIntegration.isLoaded()) {
            return PasteResult.failure("message.another_quarries.quarry.filter.copier.ad_missing");
        }
        if (!QuarryFilterModuleData.isFilterModule(player.getItemInHand(hand))) {
            return PasteResult.failure("message.another_quarries.quarry.filter.copier.missing");
        }
        ItemStack copier = findSettingsCopier(player);
        if (copier == null) {
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
        if (!isItemFilterListSnapshot(snapshot)) {
            return PasteResult.failure("message.another_quarries.quarry.filter.copier.not_filter");
        }
        List<String> imported = readLines(snapshot);
        QuarryFilterModuleData.replaceAllInHand(player, hand, imported);
        return PasteResult.success("message.another_quarries.quarry.filter.copier.pasted", imported.size());
    }

    /** First Settings Copier found in hands or inventory (client display fallback). */
    public static ItemStack findHeldSettingsCopier(Player player) {
        return findSettingsCopier(player);
    }

    private static ItemStack findSettingsCopier(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (isSettingsCopier(stack)) {
                return stack;
            }
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isSettingsCopier(stack)) {
                return stack;
            }
        }
        return null;
    }

    public static boolean isSettingsCopierItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return SETTINGS_COPIER_ITEM_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static boolean isSettingsCopier(ItemStack stack) {
        return isSettingsCopierItem(stack);
    }

    /** True when copier is in single filter-list mode (not full node ALL mode). */
    public static boolean isCopierFilterListMode(ItemStack copier) {
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
    private static DataComponentType<CompoundTag> settingsSnapshotComponentType() {
        if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(AD_SETTINGS_SNAPSHOT_COMPONENT)) {
            return null;
        }
        return (DataComponentType<CompoundTag>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(AD_SETTINGS_SNAPSHOT_COMPONENT);
    }

    @SuppressWarnings("unchecked")
    private static DataComponentType<Boolean> copierFilterModeComponentType() {
        if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(AD_COPIER_FILTER_MODE_COMPONENT)) {
            return null;
        }
        return (DataComponentType<Boolean>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(AD_COPIER_FILTER_MODE_COMPONENT);
    }

    private static CompoundTag buildFilterListSnapshot(List<String> lines) {
        List<String> safe = new ArrayList<>();
        int max = ModConfig.quarryFilterMaxLines();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String sanitized = QuarryItemFilterMatcher.sanitizeLine(line);
            if (!sanitized.isEmpty()) {
                safe.add(sanitized);
            }
            if (safe.size() >= max) {
                break;
            }
        }
        CompoundTag root = new CompoundTag();
        root.putInt(KEY_FMT, FORMAT_VERSION);
        root.putByte(KEY_KIND, STORE_KIND_FILTER);
        root.putByte(KEY_MATERIAL_KIND, MATERIAL_KIND_ITEM);
        ListTag list = new ListTag();
        for (String line : safe) {
            list.add(StringTag.valueOf(line));
        }
        root.put(KEY_LINES, list);
        return root;
    }

    private static boolean isItemFilterListSnapshot(CompoundTag tag) {
        if (tag == null || !tag.contains(KEY_FMT)) {
            return false;
        }
        int fmt = tag.getInt(KEY_FMT);
        if (fmt != FORMAT_VERSION && fmt != 2) {
            return false;
        }
        if (!tag.contains(KEY_KIND) || tag.getByte(KEY_KIND) != STORE_KIND_FILTER) {
            return false;
        }
        if (tag.contains(KEY_MATERIAL_KIND)) {
            return tag.getByte(KEY_MATERIAL_KIND) == MATERIAL_KIND_ITEM;
        }
        return true;
    }

    private static List<String> readLines(CompoundTag tag) {
        List<String> out = new ArrayList<>();
        if (!tag.contains(KEY_LINES)) {
            return out;
        }
        ListTag list = tag.getList(KEY_LINES, net.minecraft.nbt.Tag.TAG_STRING);
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

    public record PasteResult(boolean success, String messageKey, int lineCount) {
        public static PasteResult success(String key, int lines) {
            return new PasteResult(true, key, lines);
        }

        public static PasteResult failure(String key) {
            return new PasteResult(false, key, 0);
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
