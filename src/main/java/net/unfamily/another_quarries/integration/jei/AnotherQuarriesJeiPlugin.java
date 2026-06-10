package net.unfamily.another_quarries.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.client.gui.QuarryFilterModuleScreen;
import net.unfamily.another_quarries.integration.jei.ghost.QuarryGhostIngredientHandler;

@JeiPlugin
public final class AnotherQuarriesJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
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
