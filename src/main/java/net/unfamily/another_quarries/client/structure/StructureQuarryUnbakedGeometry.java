package net.unfamily.another_quarries.client.structure;

import java.util.function.Function;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.model.EmptyModel;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.client.CompositeGeometry;

public final class StructureQuarryUnbakedGeometry implements IUnbakedGeometry<StructureQuarryUnbakedGeometry> {
    private static final ResourceLocation MODEL_DEFAULT =
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "block/structure_quarry_default");
    private static final ResourceLocation MODEL_LINE =
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "block/structure_quarry_line");

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, net.minecraft.client.renderer.texture.TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            net.minecraft.client.renderer.block.model.ItemOverrides overrides) {
        CompositeGeometry geometry = CompositeGeometry.bake(
                MODEL_DEFAULT,
                MODEL_LINE,
                StructureQuarryTextures.BLOCK_TEXTURE.toString(),
                spriteGetter);
        BakedModel base = baker.bake(MODEL_LINE, modelState);
        if (base == null) {
            base = EmptyModel.BAKED;
        }
        var particle = spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, StructureQuarryTextures.BLOCK_TEXTURE));
        return new StructureQuarryBakedModel(base, geometry, particle);
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        for (ResourceLocation dep : new ResourceLocation[] {MODEL_DEFAULT, MODEL_LINE}) {
            UnbakedModel model = modelGetter.apply(dep);
            if (model != null) {
                model.resolveParents(modelGetter);
            }
        }
    }
}
