package net.unfamily.another_quarries.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.unfamily.another_quarries.AnotherQuarries;

/** Blit helpers for quarry destroy-filter GUI textures (AD-aligned art in another_quarries namespace). */
public final class QuarryFilterGuiSprites {
    public static final Identifier ENTRY_ROW =
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/filter_entry_row.png");
    public static final Identifier SINGLE_SLOT =
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/single_slot.png");
    public static final Identifier SCROLLBAR =
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/scrollbar.png");
    public static final Identifier SINGLE_SLOT_COPY =
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/single_slot_copy.png");
    public static final Identifier FILTER_BACKGROUND =
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/backgrounds/filter.png");
    public static final Identifier VALID_KEYS_BACKGROUND =
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/backgrounds/filter_valid_keys.png");

    private static final int SLOT_SIZE = 18;

    private QuarryFilterGuiSprites() {}

    public static void blitEntryRow(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ENTRY_ROW, x, y, 0.0F, 0.0F,
                QuarryFilterGuiLayout.ENTRY_WIDTH, QuarryFilterGuiLayout.ENTRY_HEIGHT,
                QuarryFilterGuiLayout.ENTRY_WIDTH, QuarryFilterGuiLayout.ENTRY_HEIGHT);
    }

    public static void blitSingleSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT, x, y, 0.0F, 0.0F,
                SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
    }

    public static void blitCopierSlotFrame(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT_COPY, x, y, 0.0F, 0.0F,
                SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
    }
}
