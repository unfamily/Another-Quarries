package net.unfamily.another_quarries.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.client.gui.QuarryFilterModuleMenus;
import net.unfamily.another_quarries.registry.ModItems;

/** Opens the destroy-list editor for this module stack. Shift+click on a quarry installs instead. */
public final class QuarryFilterModuleItem extends DescribedItem {
    public QuarryFilterModuleItem(Properties properties, String... tooltipKeys) {
        super(properties, tooltipKeys);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.MODULE_FILTER.get())) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            QuarryFilterModuleMenus.open(serverPlayer, hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            if (level.getBlockEntity(pos) instanceof QuarryBlockEntity quarry) {
                if (level.isClientSide()) {
                    return InteractionResult.SUCCESS;
                }
                if (QuarryEquipmentInstaller.tryInstallFromHand(quarry, player, context.getHand())) {
                    return InteractionResult.CONSUME;
                }
                return InteractionResult.FAIL;
            }
        }
        if (!context.getItemInHand().is(ModItems.MODULE_FILTER.get())) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            QuarryFilterModuleMenus.open(serverPlayer, context.getHand());
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
