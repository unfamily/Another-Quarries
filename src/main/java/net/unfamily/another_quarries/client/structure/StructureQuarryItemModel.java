package net.unfamily.another_quarries.client.structure;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import net.unfamily.another_quarries.client.CompositeGeometry;
import org.joml.Matrix4fc;

/**
 * Item/hand preview matches AD {@code ProjectDuctBakedModel#itemQuads}:
 * {@link CompositeGeometry#appendForWorld} with north+south pipe mask, display transforms from
 * {@code block/structure_quarry_line} via the vanilla-baked {@link CuboidItemModelWrapper}.
 */
public final class StructureQuarryItemModel {
    /** North + south arms for item preview line (AD {@code ProjectDuctBakedModel.ITEM_PIPE_MASK}). */
    private static final int ITEM_PIPE_MASK =
            (1 << Direction.NORTH.ordinal()) | (1 << Direction.SOUTH.ordinal());

    private StructureQuarryItemModel() {}

    public static ItemModel applyLinePreview(CompositeGeometry geometry, ItemModel bakedItemModel) {
        if (!geometry.isBuilt() || !(bakedItemModel instanceof CuboidItemModelWrapper base)) {
            return bakedItemModel;
        }
        List<BakedQuad> previewQuads = new ArrayList<>();
        geometry.appendForWorld(previewQuads, ITEM_PIPE_MASK, 0);
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (BakedQuad quad : previewQuads) {
            builder.addUnculledFace(quad);
        }
        return new CuboidItemModelWrapper(
                readField(base, "tints", List.class),
                builder.build(),
                readField(base, "properties", ModelRenderProperties.class),
                readField(base, "transformation", Matrix4fc.class));
    }

    @SuppressWarnings("unchecked")
    private static <T> T readField(CuboidItemModelWrapper wrapper, String name, Class<T> type) {
        try {
            Field field = CuboidItemModelWrapper.class.getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(wrapper));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to read CuboidItemModelWrapper." + name, ex);
        }
    }
}
