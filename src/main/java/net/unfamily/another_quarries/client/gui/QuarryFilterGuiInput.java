package net.unfamily.another_quarries.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

/** Routes keyboard input to the filter edit box while edit mode is active. */
public final class QuarryFilterGuiInput {
    private QuarryFilterGuiInput() {}

    public static boolean handleFilterEditKeyPressed(
            AbstractContainerScreen<?> screen,
            KeyEvent event,
            EditBox editBox,
            Runnable onApply) {
        if (editBox == null || !editBox.visible) {
            return false;
        }
        editBox.setFocused(true);
        if (editBox.keyPressed(event)) {
            return true;
        }
        if (event.key() == InputConstants.KEY_RETURN) {
            onApply.run();
            return true;
        }
        Minecraft minecraft = screen.getMinecraft();
        if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
            return true;
        }
        return false;
    }

    public static boolean handleFilterEditCharTyped(EditBox editBox, CharacterEvent event) {
        if (editBox == null || !editBox.visible) {
            return false;
        }
        editBox.setFocused(true);
        return editBox.charTyped(event);
    }
}
