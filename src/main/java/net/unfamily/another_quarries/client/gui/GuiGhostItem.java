package net.unfamily.another_quarries.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class GuiGhostItem {
    public static final int DEFAULT_ARGB = 0x80FFFFFF;

    private GuiGhostItem() {}

    public static void render(GuiGraphicsExtractor graphics, int leftPos, int topPos, Slot slot, ItemStack ghostStack) {
        render(graphics, leftPos, topPos, slot, ghostStack, DEFAULT_ARGB);
    }

    public static void render(GuiGraphicsExtractor graphics, int leftPos, int topPos, Slot slot, ItemStack ghostStack, int argbColor) {
        if (!shouldRenderGhost(slot) || ghostStack.isEmpty()) {
            return;
        }
        GhostItemRenderer.render(graphics, ghostStack, leftPos + slot.x, topPos + slot.y, argbColor);
    }

    public static void renderCycling(
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos,
            Slot slot,
            List<ItemStack> cycleStacks,
            GuiCycleTimer timer,
            int argbColor) {
        if (!shouldRenderGhost(slot) || cycleStacks.isEmpty()) {
            return;
        }
        timer.onDraw();
        ItemStack stack = timer.getOrDefault(cycleStacks, cycleStacks.get(0));
        GhostItemRenderer.render(graphics, stack, leftPos + slot.x, topPos + slot.y, argbColor);
    }

    private static boolean shouldRenderGhost(Slot slot) {
        return slot != null && slot.getItem().isEmpty();
    }
}
