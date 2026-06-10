package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.another_quarries.config.ModConfig;

import java.util.List;

public final class QuarryBlockBreaker {
    private static final TagKey<net.minecraft.world.level.block.Block> ORES =
            TagKey.create(Registries.BLOCK, net.minecraft.resources.ResourceLocation.withDefaultNamespace("ores"));

    private QuarryBlockBreaker() {}

    public static int breakTicksForBlock(net.minecraft.world.level.Level level, BlockPos pos, QuarryWorkContext ctx) {
        int ticks = ModConfig.baseBreakTicks();
        float multiplier = 1.0F;
        multiplier *= ModConfig.speedFactor(ctx.speedModules());
        BlockState state = level.getBlockState(pos);
        if (!state.is(ORES) && ctx.diggerModules() > 0) {
            multiplier *= ModConfig.diggerFactor(ctx.diggerModules());
        }
        multiplier *= ctx.drill().speedMultiplier();
        return Math.max(1, Math.round(ticks / multiplier));
    }

    public static boolean canBreak(net.minecraft.world.level.Level level, BlockPos pos, QuarryDrillType drill) {
        return QuarryMiningFilters.isMineable(level, pos, drill.maxMiningLevel());
    }

    public static boolean breakBlock(ServerLevel level, BlockPos pos, QuarryWorkContext ctx, ItemStackHandler buffer) {
        if (!canBreak(level, pos, ctx.drill())) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        ItemStack tool = ctx.breakTool().isEmpty() ? buildTool(ctx, level) : ctx.breakTool();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, null, tool);
        if (drops.isEmpty()) {
            drops = Block.getDrops(state, level, pos, blockEntity, null, ItemStack.EMPTY);
        }
        if (!level.removeBlock(pos, false)) {
            return false;
        }
        var registries = level.registryAccess();
        for (ItemStack drop : drops) {
            if (ctx.voidFilteredDrops()
                    && QuarryItemFilterMatcher.matchesAny(ctx.itemDenyFilters(), drop, registries)) {
                continue;
            }
            insertIntoBuffer(buffer, drop);
        }
        return true;
    }

    public static ItemStack buildBreakTool(QuarryWorkContext ctx, ServerLevel level) {
        return buildTool(ctx, level);
    }

    private static ItemStack buildTool(QuarryWorkContext ctx, ServerLevel level) {
        ItemStack tool = ctx.drill().toolTemplate();
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (ctx.silkTouch()) {
            tool.enchant(registry.getOrThrow(Enchantments.SILK_TOUCH), 1);
        } else if (ctx.fortuneLevel() > 0) {
            tool.enchant(registry.getOrThrow(Enchantments.FORTUNE), ctx.fortuneLevel());
        }
        return tool;
    }

    private static void insertIntoBuffer(ItemStackHandler buffer, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < buffer.getSlots(); i++) {
            remaining = buffer.insertItem(i, remaining, false);
            if (remaining.isEmpty()) {
                return;
            }
        }
    }
}
