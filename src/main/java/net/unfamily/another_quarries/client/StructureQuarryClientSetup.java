package net.unfamily.another_quarries.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.client.gui.QuarryFilterModuleScreen;
import net.unfamily.another_quarries.client.gui.QuarryScreen;
import net.unfamily.another_quarries.client.structure.StructureQuarryGeometryLoader;
import net.unfamily.another_quarries.registry.ModMenuTypes;

@EventBusSubscriber(modid = AnotherQuarries.MOD_ID, value = Dist.CLIENT)
public final class StructureQuarryClientSetup {
    private StructureQuarryClientSetup() {}

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.QUARRY_MENU.get(), QuarryScreen::new);
        event.register(ModMenuTypes.QUARRY_FILTER_MODULE_MENU.get(), QuarryFilterModuleScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(StructureQuarryGeometryLoader.ID, new StructureQuarryGeometryLoader());
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "block/structure_quarry_default")));
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "block/structure_quarry_line")));
    }
}
