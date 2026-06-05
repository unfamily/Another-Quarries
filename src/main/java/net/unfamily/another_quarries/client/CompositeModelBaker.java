package net.unfamily.another_quarries.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.google.common.collect.Interners;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3fc;

/**
 * Minimal {@link ModelBaker} used only to bake composite cuboid templates during model load.
 */
final class CompositeModelBaker implements ModelBaker {
    private final MaterialBaker materials;
    private final ModelBaker.Interner interner = new SimpleInterner();
    private final Map<SharedOperationKey<Object>, Object> operationCache = new ConcurrentHashMap<>();

    private CompositeModelBaker(MaterialBaker materials) {
        this.materials = materials;
    }

    static CompositeModelBaker fromTextureGetter(Function<Identifier, TextureAtlasSprite> textureGetter) {
        MaterialBaker materialBaker = new MaterialBaker() {
            @Override
            public Material.Baked get(Material material, ModelDebugName name) {
                return new Material.Baked(textureGetter.apply(material.sprite()), material.forceTranslucent());
            }

            @Override
            public Material.Baked reportMissingReference(String reference, ModelDebugName name) {
                return get(new Material(Identifier.withDefaultNamespace("missingno")), name);
            }
        };
        return new CompositeModelBaker(materialBaker);
    }

    @Override
    public ResolvedModel getModel(Identifier location) {
        throw new UnsupportedOperationException("CompositeModelBaker does not resolve models: " + location);
    }

    @Override
    public BlockStateModelPart missingBlockModelPart() {
        throw new UnsupportedOperationException("CompositeModelBaker has no missing model part");
    }

    @Override
    public MaterialBaker materials() {
        return materials;
    }

    @Override
    public Interner interner() {
        return interner;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T compute(SharedOperationKey<T> key) {
        return (T) operationCache.computeIfAbsent((SharedOperationKey<Object>) key, k -> k.compute(this));
    }

    private static final class SimpleInterner implements ModelBaker.Interner {
        private final com.google.common.collect.Interner<Vector3fc> vectors = Interners.newStrongInterner();
        private final com.google.common.collect.Interner<BakedQuad.MaterialInfo> materialInfos = Interners.newStrongInterner();
        private final com.google.common.collect.Interner<BakedNormals> normals = Interners.newStrongInterner();
        private final com.google.common.collect.Interner<BakedColors> colors = Interners.newStrongInterner();

        @Override
        public Vector3fc vector(Vector3fc vector) {
            return vectors.intern(vector);
        }

        @Override
        public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo material) {
            return materialInfos.intern(material);
        }

        @Override
        public BakedNormals normals(BakedNormals bakedNormals) {
            return normals.intern(bakedNormals);
        }

        @Override
        public BakedColors colors(BakedColors bakedColors) {
            return colors.intern(bakedColors);
        }
    }
}
