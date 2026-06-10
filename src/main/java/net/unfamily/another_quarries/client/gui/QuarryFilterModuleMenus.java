package net.unfamily.another_quarries.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.unfamily.another_quarries.network.ModNetworking;

public final class QuarryFilterModuleMenus {
    private QuarryFilterModuleMenus() {}

    public static void open(ServerPlayer player, InteractionHand hand) {
        if (!QuarryFilterModuleMenu.isValidHandStack(player, hand)) {
            return;
        }
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.another_quarries.quarry.filter.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player ignored) {
                return new QuarryFilterModuleMenu(containerId, playerInventory, hand);
            }
        }, buf -> InteractionHand.STREAM_CODEC.encode(buf, hand));
        ModNetworking.sendFilterBulkSync(player, hand);
    }
}
