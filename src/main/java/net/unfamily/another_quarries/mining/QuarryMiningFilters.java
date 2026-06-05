package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.unfamily.another_quarries.config.ModConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Config-driven mining filters plus automatic skips for fluids and container blocks.
 * List entries may be block IDs ({@code minecraft:spawner}) or block tags ({@code #c:ores/allthemodium}).
 */
public final class QuarryMiningFilters {
    private static Set<Block> denyBlocks = Set.of();
    private static Set<TagKey<Block>> denyTags = Set.of();
    private static Set<Block> allowBlocks = Set.of();
    private static Set<TagKey<Block>> allowTags = Set.of();
    private static boolean allowListActive;
    private static boolean skipInventories = true;

    private QuarryMiningFilters() {}

    public static void reload() {
        FilterEntries deny = parseFilterEntries(ModConfig.miningDenyList());
        denyBlocks = deny.blocks();
        denyTags = deny.tags();

        FilterEntries allow = parseFilterEntries(ModConfig.miningAllowList());
        allowBlocks = allow.blocks();
        allowTags = allow.tags();
        allowListActive = !allowBlocks.isEmpty() || !allowTags.isEmpty();
        skipInventories = ModConfig.skipInventories();
    }

    public static boolean isMineable(Level level, BlockPos pos) {
        return isMineable(level, pos, QuarryMiningLevels.TIER_MODDED);
    }

    public static boolean isMineable(Level level, BlockPos pos, int maxMiningLevel) {
        if (level.isEmptyBlock(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LiquidBlock) {
            return false;
        }
        if (!state.getFluidState().isEmpty() && state.canBeReplaced()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        Block block = state.getBlock();
        if (matchesDenyList(block)) {
            return false;
        }
        if (allowListActive && !matchesAllowList(block)) {
            return false;
        }
        if (skipInventories && hasInventoryToSkip(level, pos, state)) {
            return false;
        }
        return QuarryMiningLevels.isWithinMiningLevel(state, maxMiningLevel);
    }

    private static boolean matchesDenyList(Block block) {
        if (denyBlocks.contains(block)) {
            return true;
        }
        return matchesAnyTag(block.builtInRegistryHolder(), denyTags);
    }

    private static boolean matchesAllowList(Block block) {
        if (allowBlocks.contains(block)) {
            return true;
        }
        return matchesAnyTag(block.builtInRegistryHolder(), allowTags);
    }

    private static boolean matchesAnyTag(Holder<Block> holder, Set<TagKey<Block>> tags) {
        for (TagKey<Block> tag : tags) {
            if (holder.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInventoryToSkip(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof ShulkerBoxBlock) {
            return true;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }
        if (blockEntity instanceof Container) {
            return true;
        }
        var itemHandler = level.getCapability(Capabilities.Item.BLOCK, pos, state, blockEntity, null);
        return itemHandler != null && itemHandler.size() > 0;
    }

    private record FilterEntries(Set<Block> blocks, Set<TagKey<Block>> tags) {}

    private static FilterEntries parseFilterEntries(List<? extends String> entries) {
        Set<Block> blocks = new HashSet<>();
        Set<TagKey<Block>> tags = new HashSet<>();
        for (String raw : entries) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(trimmed.substring(1));
                if (tagId != null) {
                    tags.add(TagKey.create(Registries.BLOCK, tagId));
                }
            } else {
                Identifier blockId = Identifier.tryParse(trimmed);
                if (blockId != null) {
                    BuiltInRegistries.BLOCK.getOptional(blockId).ifPresent(blocks::add);
                }
            }
        }
        return new FilterEntries(Set.copyOf(blocks), Set.copyOf(tags));
    }
}
