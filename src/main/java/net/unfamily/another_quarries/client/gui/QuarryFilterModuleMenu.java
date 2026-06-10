package net.unfamily.another_quarries.client.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.another_quarries.integration.anotherdynamics.AnotherDynamicsIntegration;
import net.unfamily.another_quarries.integration.anotherdynamics.QuarryFilterCopierCompat;
import net.unfamily.another_quarries.item.QuarryFilterModuleData;
import net.unfamily.another_quarries.network.InteractionHandNetworking;
import net.unfamily.another_quarries.registry.ModMenuTypes;

import java.util.ArrayList;
import java.util.List;

public class QuarryFilterModuleMenu extends AbstractContainerMenu {
    public static final int PLAYER_SLOT_COUNT = 36;

    private final InteractionHand editHand;
    private final List<String> clientDestroyFilters = new ArrayList<>();
    private final ItemStackHandler copierHandler = new ItemStackHandler(1);
    private int copySettingsSlotIndex = -1;
    private int playerSlotStart = -1;
    private boolean clientDestroyFiltersDirty;

    public static QuarryFilterModuleMenu create(int containerId, Inventory playerInventory, FriendlyByteBuf extra) {
        InteractionHand hand = InteractionHandNetworking.decode(extra);
        return new QuarryFilterModuleMenu(containerId, playerInventory, hand);
    }

    public QuarryFilterModuleMenu(int containerId, Inventory playerInventory, InteractionHand editHand) {
        super(ModMenuTypes.QUARRY_FILTER_MODULE_MENU.get(), containerId);
        this.editHand = editHand;
        addCopierSlotIfNeeded();
        playerSlotStart = slots.size();
        addPlayerInventory(playerInventory);
    }

    public InteractionHand getEditHand() {
        return editHand;
    }

    public static boolean isValidHandStack(Player player, InteractionHand hand) {
        return QuarryFilterModuleData.isFilterModule(player.getItemInHand(hand));
    }

    public int copySettingsSlotIndex() {
        return copySettingsSlotIndex;
    }

    public int playerSlotStart() {
        return playerSlotStart;
    }

    private void addCopierSlotIfNeeded() {
        if (!AnotherDynamicsIntegration.isLoaded() || copySettingsSlotIndex >= 0) {
            return;
        }
        copySettingsSlotIndex = slots.size();
        addSlot(new SlotItemHandler(copierHandler, 0,
                QuarryFilterGuiLayout.SLOT_COPY_X, QuarryFilterGuiLayout.SLOT_COPY_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return QuarryFilterCopierCompat.isSettingsCopierItem(stack);
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int startX = QuarryFilterGuiLayout.PLAYER_SLOTS_X;
        int startY = QuarryFilterGuiLayout.PLAYER_SLOTS_Y;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                addSlot(new Slot(playerInventory, index, startX + col * 18, startY + row * 18));
            }
        }
        int hotbarY = startY + 3 * 18 + QuarryFilterGuiLayout.HOTBAR_GAP;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, startX + col * 18, hotbarY));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return isValidHandStack(player, editHand);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (playerSlotStart < 0) {
            return ItemStack.EMPTY;
        }
        int playerEnd = playerSlotStart + PLAYER_SLOT_COUNT;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (copySettingsSlotIndex >= 0 && index == copySettingsSlotIndex) {
            if (moveItemStackTo(stack, playerSlotStart, playerEnd, false)) {
                return original;
            }
            return ItemStack.EMPTY;
        }

        if (index >= playerSlotStart && index < playerEnd) {
            if (copySettingsSlotIndex >= 0
                    && QuarryFilterCopierCompat.isSettingsCopierItem(stack)
                    && moveItemStackTo(stack, copySettingsSlotIndex, copySettingsSlotIndex + 1, false)) {
                return original;
            }
        }

        return ItemStack.EMPTY;
    }

    public void applyClientDestroyFilters(List<String> lines) {
        clientDestroyFilters.clear();
        if (lines != null) {
            clientDestroyFilters.addAll(lines);
        }
        clientDestroyFiltersDirty = true;
    }

    public boolean consumeClientDestroyFiltersDirty() {
        if (clientDestroyFiltersDirty) {
            clientDestroyFiltersDirty = false;
            return true;
        }
        return false;
    }

    public List<String> getClientDestroyFilters() {
        return clientDestroyFilters;
    }

    public void setClientDestroyFilterLine(int index, String text) {
        while (clientDestroyFilters.size() <= index) {
            clientDestroyFilters.add("");
        }
        clientDestroyFilters.set(index, text != null ? text : "");
    }
}
