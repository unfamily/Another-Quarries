package net.unfamily.another_quarries.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.client.gui.QuarryFilterModuleScreen;
import net.unfamily.another_quarries.integration.jei.ghost.QuarryGhostIngredientHandler;

@JeiPlugin
public final class AnotherQuarriesJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_ID =
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(
                QuarryFilterModuleScreen.class,
                new QuarryGhostIngredientHandler<>());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JeiRuntimeState.setRuntime(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiRuntimeState.clearRuntime();
    }
}
