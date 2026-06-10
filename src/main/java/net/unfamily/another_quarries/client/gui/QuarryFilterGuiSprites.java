package net.unfamily.another_quarries.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.another_quarries.AnotherQuarries;

/** Blit helpers for quarry destroy-filter GUI textures (AD-aligned art in another_quarries namespace). */
public final class QuarryFilterGuiSprites {
    public static final ResourceLocation ENTRY_ROW =
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/filter_entry_row.png");
    public static final ResourceLocation SINGLE_SLOT =
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/single_slot.png");
    public static final ResourceLocation SCROLLBAR =
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/scrollbar.png");
    public static final ResourceLocation SINGLE_SLOT_COPY =
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/single_slot_copy.png");
    public static final ResourceLocation FILTER_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/backgrounds/filter.png");
    public static final ResourceLocation VALID_KEYS_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/backgrounds/filter_valid_keys.png");

    private static final int SLOT_SIZE = 18;

    private QuarryFilterGuiSprites() {}

    public static void blitEntryRow(GuiGraphics graphics, int x, int y) {
        graphics.blit(ENTRY_ROW, x, y, 0, 0,
                QuarryFilterGuiLayout.ENTRY_WIDTH, QuarryFilterGuiLayout.ENTRY_HEIGHT,
                QuarryFilterGuiLayout.ENTRY_WIDTH, QuarryFilterGuiLayout.ENTRY_HEIGHT);
    }

    public static void blitSingleSlot(GuiGraphics graphics, int x, int y) {
        graphics.blit(SINGLE_SLOT, x, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
    }

    public static void blitCopierSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.blit(SINGLE_SLOT_COPY, x, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
    }
}
