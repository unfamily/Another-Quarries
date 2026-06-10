package net.unfamily.another_quarries.client.gui;

/** Pixel layout for the quarry destroy-filter panel (matches Another Dynamics deny-filter view on 320x256). */
public final class QuarryFilterGuiLayout {
    public static final int PANEL_WIDTH = 320;
    public static final int PANEL_HEIGHT = 256;

    public static final int CENTER_X = 38;
    public static final int ENTRY_X = CENTER_X;
    public static final int ENTRY_WIDTH = 220;
    public static final int ENTRY_HEIGHT = 24;
    public static final int FIRST_FILTER_ROW_Y = 32;
    public static final int VISIBLE_FILTER_ENTRIES = 4;

    public static final int SCROLLBAR_WIDTH = 8;
    public static final int SCROLLBAR_HEIGHT = 34;
    public static final int HANDLE_SIZE = 8;
    public static final int SCROLLBAR_TEXTURE_WIDTH = 32;
    public static final int SCROLLBAR_TEXTURE_HEIGHT = 34;

    public static final int SCROLLBAR_X_REL = ENTRY_X + ENTRY_WIDTH + 4;
    public static final int BUTTON_UP_Y_REL = FIRST_FILTER_ROW_Y;
    public static final int SCROLLBAR_Y_REL = BUTTON_UP_Y_REL + HANDLE_SIZE;
    public static final int BUTTON_DOWN_Y_REL = SCROLLBAR_Y_REL + SCROLLBAR_HEIGHT;

    public static final int SLOT_GEOMETRY_NUDGE = 2;
    public static final int SLOT_COPY_BACKGROUND_X = 278 + SLOT_GEOMETRY_NUDGE;
    public static final int CHANNEL_BACKGROUND_Y = 52 + SLOT_GEOMETRY_NUDGE;
    public static final int COPY_COLUMN_GAP = 2;
    public static final int SLOT_COPY_BACKGROUND_Y = CHANNEL_BACKGROUND_Y + 18 + COPY_COLUMN_GAP;
    public static final int COPIER_ACTION_BUTTON_H = 12;
    public static final int COPIER_SAVE_BUTTON_Y = SLOT_COPY_BACKGROUND_Y + 18 + COPY_COLUMN_GAP;
    public static final int COPIER_LOAD_BUTTON_Y = COPIER_SAVE_BUTTON_Y + COPIER_ACTION_BUTTON_H + COPY_COLUMN_GAP;

    private static final int SLOT_CONTENT_LAYER_DX = 1;
    private static final int SLOT_CONTENT_LAYER_DY = 1;
    public static final int SLOT_COPY_X = SLOT_COPY_BACKGROUND_X + SLOT_CONTENT_LAYER_DX;
    public static final int SLOT_COPY_Y = SLOT_COPY_BACKGROUND_Y + SLOT_CONTENT_LAYER_DY;

    public static final int FILTER_NAV_GAP = 4;
    public static final int EDIT_MODE_GAP_BELOW_LIST = 4;
    public static final int EDIT_MODE_TEXT_INSET_X = 10;
    public static final int ADVANCED_FILTER_BUTTON_WIDTH = 64;
    public static final int ADJACENT_BTN_GAP = 2;
    public static final int BTN_H = 14;

    public static final int HELP_BACK_BUTTON_X = 8;
    public static final int HELP_BACK_BUTTON_Y = PANEL_HEIGHT - 25;

    public static final int CLOSE_BUTTON_SIZE = 12;
    public static final int CLOSE_BUTTON_X = PANEL_WIDTH - CLOSE_BUTTON_SIZE - 5;
    public static final int CLOSE_BUTTON_Y = 5;
    public static final int TITLE_Y = 7;

    /** Player inventory band (matches Another Dynamics DuctNodeMenu). */
    public static final int PLAYER_SLOTS_X = 80;
    public static final int PLAYER_SLOTS_Y = 171;
    public static final int HOTBAR_GAP = 4;

    public static final int FILTER_ENTRY_TEXT_COLOR = 0xFF404040;
    public static final int FILTER_EDIT_TEXT_COLOR = 0xFFFFFFFF;
    public static final int ENTRY_SLOT_INSET = 3;
    public static final int ROW_BUTTON_SIZE = 12;
    public static final int ROW_BUTTON_MARGIN = 4;
    public static final int ROW_BUTTON_SPACING = 2;

    private QuarryFilterGuiLayout() {}

    public static int filterNavRowY() {
        return FIRST_FILTER_ROW_Y + VISIBLE_FILTER_ENTRIES * ENTRY_HEIGHT + FILTER_NAV_GAP;
    }

    public static int editModeRowAnchorY() {
        return FIRST_FILTER_ROW_Y + VISIBLE_FILTER_ENTRIES * ENTRY_HEIGHT + EDIT_MODE_GAP_BELOW_LIST;
    }

    /** X of Back / Valid keys row (aligned with edit-mode advanced button slot in AD). */
    public static int navBackButtonX() {
        int buttonSize = ROW_BUTTON_SIZE;
        int buttonSpacing = ROW_BUTTON_SPACING;
        int slotSize = 18;
        int rowLeft = ENTRY_X;
        int slotX = rowLeft + buttonSize + buttonSpacing;
        int rightArrowX = slotX + slotSize + buttonSpacing;
        return rightArrowX + buttonSize + buttonSpacing;
    }

    public static int navBackButtonY() {
        int slotSize = 18;
        int slotY = editModeRowAnchorY();
        return slotY + (slotSize - BTN_H) / 2;
    }
}
