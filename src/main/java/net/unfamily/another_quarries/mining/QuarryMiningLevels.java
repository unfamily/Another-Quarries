package net.unfamily.another_quarries.mining;

import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/**
 * Mining tier detection aligned with Iskandert scanner ore filtering.
 * 0 = any, 1 = wood, 2 = stone, 3 = iron, 4 = diamond, 5 = netherite, 100 = above netherite.
 * <p>
 * The scanner bumps untagged ores ({@link Tags.Blocks#ORES} with tier 0/1) to 100 for chip
 * filtering. The quarry keeps their raw tier so they remain mineable with normal drills.
 */
public final class QuarryMiningLevels {
    public static final int TIER_ANY = 0;
    public static final int TIER_WOOD = 1;
    public static final int TIER_STONE = 2;
    public static final int TIER_IRON = 3;
    public static final int TIER_DIAMOND = 4;
    public static final int TIER_NETHERITE = 5;
    public static final int TIER_MODDED = 100;

    private QuarryMiningLevels() {}

    public static int getBlockMiningLevel(BlockState blockState) {
        Holder<Block> holder = blockState.getBlock().builtInRegistryHolder();

        if (holder.is(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)) {
            return TIER_MODDED;
        }
        if (holder.is(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)) {
            return TIER_NETHERITE;
        }
        if (holder.is(Tags.Blocks.NEEDS_NETHERITE_TOOL)) {
            return TIER_NETHERITE;
        }
        if (holder.is(BlockTags.INCORRECT_FOR_IRON_TOOL)) {
            return TIER_DIAMOND;
        }
        if (holder.is(BlockTags.INCORRECT_FOR_STONE_TOOL)) {
            return TIER_IRON;
        }
        if (holder.is(BlockTags.INCORRECT_FOR_WOODEN_TOOL)) {
            return TIER_STONE;
        }

        int tier = TIER_ANY;
        if (blockState.requiresCorrectToolForDrops()) {
            if (holder.is(BlockTags.MINEABLE_WITH_PICKAXE)
                    || holder.is(BlockTags.MINEABLE_WITH_AXE)
                    || holder.is(BlockTags.MINEABLE_WITH_SHOVEL)
                    || holder.is(BlockTags.MINEABLE_WITH_HOE)) {
                tier = TIER_WOOD;
            }
        }

        // Scanner maps untagged ores to 100 for chip filters; quarry keeps raw tier (0/1).
        return tier;
    }

    public static boolean isWithinMiningLevel(BlockState state, int maxMiningLevel) {
        if (maxMiningLevel >= TIER_MODDED) {
            return true;
        }
        return getBlockMiningLevel(state) <= maxMiningLevel;
    }
}
