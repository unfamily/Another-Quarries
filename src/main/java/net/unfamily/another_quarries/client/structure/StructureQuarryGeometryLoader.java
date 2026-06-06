package net.unfamily.another_quarries.client.structure;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.another_quarries.AnotherQuarries;

public final class StructureQuarryGeometryLoader
        implements net.neoforged.neoforge.client.model.geometry.IGeometryLoader<StructureQuarryUnbakedGeometry> {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "structure_quarry");

    @Override
    public StructureQuarryUnbakedGeometry read(
            com.google.gson.JsonObject jsonObject,
            com.google.gson.JsonDeserializationContext deserializationContext) {
        return new StructureQuarryUnbakedGeometry();
    }
}
