package net.unfamily.another_quarries.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Routes keyboard input to the filter edit box while edit mode is active. */
public final class QuarryFilterGuiInput {
    private QuarryFilterGuiInput() {}

    public static boolean handleFilterEditKeyPressed(
            AbstractContainerScreen<?> screen,
            int keyCode,
            int scanCode,
            int modifiers,
            EditBox editBox,
            Runnable onApply) {
        if (editBox == null || !editBox.visible) {
            return false;
        }
        editBox.setFocused(true);
        if (editBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN) {
            onApply.run();
            return true;
        }
        Minecraft minecraft = screen.getMinecraft();
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return false;
    }

    public static boolean handleFilterEditCharTyped(EditBox editBox, char codePoint, int modifiers) {
        if (editBox == null || !editBox.visible) {
            return false;
        }
        editBox.setFocused(true);
        return editBox.charTyped(codePoint, modifiers);
    }
}
