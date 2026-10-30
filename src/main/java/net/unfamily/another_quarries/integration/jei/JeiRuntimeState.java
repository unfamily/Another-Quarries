package net.unfamily.another_quarries.integration.jei;

import java.util.Optional;

import mezz.jei.api.runtime.IIngredientListOverlay;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only runtime state bridge for optional JEI integration.
 * Keyboard-focus detection matches Another Dynamics (overlay search / open recipes GUI only).
 */
public final class JeiRuntimeState {
    private static @Nullable IJeiRuntime runtime;

    private JeiRuntimeState() {}

    public static void setRuntime(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static void clearRuntime() {
        runtime = null;
    }

    /**
     * True when JEI wants keyboard input (search box focused) or its recipes GUI is currently open.
     * Used so container screens do not steal Esc/E/Backspace/Ctrl+V while JEI is actively handling them.
     */
    public static boolean jeiHasKeyboardFocusOrRecipesGuiOpen() {
        IJeiRuntime rt = runtime;
        if (rt == null) {
            return false;
        }
        try {
            IIngredientListOverlay overlay = rt.getIngredientListOverlay();
            if (overlay != null && overlay.isListDisplayed() && overlay.hasKeyboardFocus()) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            IRecipesGui recipesGui = rt.getRecipesGui();
            if (recipesGui != null) {
                Optional<Screen> parent = recipesGui.getParentScreen();
                if (parent != null && parent.isPresent()) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
