package net.unfamily.another_quarries.integration.jei;

import mezz.jei.api.runtime.IJeiRuntime;
import org.jetbrains.annotations.Nullable;

public final class JeiRuntimeState {
    private static @Nullable IJeiRuntime runtime;

    private JeiRuntimeState() {}

    public static void setRuntime(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static void clearRuntime() {
        runtime = null;
    }

    public static boolean jeiHasKeyboardFocusOrRecipesGuiOpen() {
        if (runtime == null) {
            return false;
        }
        return runtime.getIngredientListOverlay().hasKeyboardFocus()
                || runtime.getRecipesGui() != null;
    }
}
