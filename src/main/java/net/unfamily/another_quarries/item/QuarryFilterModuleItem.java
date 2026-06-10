package net.unfamily.another_quarries.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.unfamily.another_quarries.client.gui.QuarryFilterModuleMenus;
import net.unfamily.another_quarries.registry.ModItems;

/** Opens the destroy-list editor for this module stack. Shift+click on a quarry installs instead. */
public final class QuarryFilterModuleItem extends DescribedItem {
    public QuarryFilterModuleItem(Properties properties, String... tooltipKeys) {
        super(properties, tooltipKeys);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).is(ModItems.MODULE_FILTER.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            QuarryFilterModuleMenus.open(serverPlayer, hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult install = QuarryEquipmentInstaller.useOnQuarryBlock(context);
        if (install != InteractionResult.PASS) {
            return install;
        }
        Player player = context.getPlayer();
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
