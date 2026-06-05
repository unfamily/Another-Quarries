package net.unfamily.another_quarries;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.unfamily.another_quarries.client.gui.QuarryScreen;
import net.unfamily.another_quarries.registry.ModMenuTypes;
import net.unfamily.another_quarries.client.CompositeGeometry;
import net.unfamily.another_quarries.client.structure.StructureQuarryBlockStateModel;
import net.unfamily.another_quarries.client.structure.StructureQuarryItemModel;
import net.unfamily.another_quarries.client.structure.StructureQuarryTextures;
import net.unfamily.another_quarries.registry.ModBlocks;

@Mod(value = AnotherQuarries.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AnotherQuarries.MOD_ID, value = Dist.CLIENT)
public final class AnotherQuarriesClient {
    private static final Identifier MODEL_DEFAULT =
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "block/structure_quarry_default");
    private static final Identifier MODEL_LINE =
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "block/structure_quarry_line");
    private static final Identifier ITEM_MODEL =
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "item/structure_quarry");
    private static final ModelDebugName DEBUG_NAME = () -> "structure_quarry";

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        AnotherQuarries.LOGGER.debug("Client setup for {}", AnotherQuarries.MOD_ID);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.QUARRY_MENU.get(), QuarryScreen::new);
    }

    @SubscribeEvent
    static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        var textureGetter = event.getTextureGetter();
        CompositeGeometry geometry = CompositeGeometry.bake(textureGetter, MODEL_DEFAULT, MODEL_LINE, StructureQuarryTextures.BLOCK_TEXTURE);
        TextureAtlasSprite particleSprite = textureGetter.apply(StructureQuarryTextures.BLOCK_TEXTURE);
        Material.Baked particle = new Material.Baked(particleSprite, false);

        var blockModels = event.getBakingResult().blockStateModels();
        for (BlockState state : blockModels.keySet().toArray(new BlockState[0])) {
            if (state.is(ModBlocks.STRUCTURE_QUARRY.get())) {
                BlockStateModel base = blockModels.get(state);
                blockModels.put(state, new StructureQuarryBlockStateModel(geometry, particle, base));
            }
        }

        ItemModel bakedItem = event.getBakingResult().itemStackModels().get(ITEM_MODEL);
        if (bakedItem != null) {
            event.getBakingResult().itemStackModels().put(ITEM_MODEL, StructureQuarryItemModel.applyLinePreview(geometry, bakedItem));
        }
    }
}
