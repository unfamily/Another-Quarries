package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.another_quarries.block.entity.QuarryBufferHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.item.QuarryEquipmentSlots;
import net.unfamily.another_quarries.registry.ModItems;

import java.util.List;

public final class QuarryBlockBreaker {
    private static final TagKey<Block> ORES =
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
        QuarryFluidRules rules = ctx.fluidRules();
        if (state.getBlock() instanceof LiquidBlock) {
            return QuarryFluidMining.handleFluidBlock(level, pos, rules);
        }
        QuarryFluidRules.Action fluidAction = QuarryFluidMining.classifyWaterlogged(state, rules);
        ItemStack tool = ctx.breakTool().isEmpty() ? buildTool(ctx, level, null) : ctx.breakTool();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, null, tool);
        if (drops.isEmpty()) {
            drops = Block.getDrops(state, level, pos, blockEntity, null, ItemStack.EMPTY);
        }
        var containedFluid = state.getFluidState().getType();
        boolean drainFluid = fluidAction == QuarryFluidRules.Action.VOID || fluidAction == QuarryFluidRules.Action.PUMP;
        if (fluidAction == QuarryFluidRules.Action.PUMP
                && (rules.sink() == null || !rules.sink().accept(containedFluid, QuarryFluidMining.SOURCE_AMOUNT_MB))) {
            drainFluid = false;
        }
        // Waterlogged blocks leave their fluid behind unless the void/pump rules take it.
        boolean removed = drainFluid
                ? level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)
                : level.removeBlock(pos, false);
        if (!removed) {
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

    public static ItemStack buildBreakTool(QuarryWorkContext ctx, ServerLevel level, ItemStackHandler equipment) {
        return buildTool(ctx, level, equipment);
    }

    /** @deprecated use {@link #buildBreakTool(QuarryWorkContext, ServerLevel, ItemStackHandler)} */
    @Deprecated
    public static ItemStack buildBreakTool(QuarryWorkContext ctx, ServerLevel level) {
        return buildTool(ctx, level, null);
    }

    private static ItemStack buildTool(QuarryWorkContext ctx, ServerLevel level, ItemStackHandler equipment) {
        ItemStack tool = ctx.drill().toolTemplate();
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (equipment != null) {
            ItemStack enchantStack = equipment.getStackInSlot(QuarryEquipmentSlots.enchantModuleSlot());
            if (enchantStack.is(ModItems.MODULE_ENCHANT.get())) {
                copyAllEnchantments(enchantStack, tool);
                return tool;
            }
        }
        if (ctx.silkTouch()) {
            tool.enchant(registry.getOrThrow(Enchantments.SILK_TOUCH), 1);
        } else if (ctx.fortuneLevel() > 0) {
            tool.enchant(registry.getOrThrow(Enchantments.FORTUNE), ctx.fortuneLevel());
        }
        return tool;
    }

    /** Copies every enchantment from the module onto the break tool (accepted via vanilla enchantable item tags). */
    private static void copyAllEnchantments(ItemStack from, ItemStack to) {
        for (var entry : from.getEnchantments().entrySet()) {
            to.enchant(entry.getKey(), entry.getIntValue());
        }
    }

    private static void insertIntoBuffer(ItemStackHandler buffer, ItemStack stack) {
        if (buffer instanceof QuarryBufferHandler quarryBuffer) {
            quarryBuffer.insertMinedItem(stack);
        }
    }
}
