package net.unfamily.another_quarries.client.structure;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.unfamily.another_quarries.block.structure.StructureQuarryBlock;
import net.unfamily.another_quarries.client.CompositeGeometry;

import org.jetbrains.annotations.Nullable;

public final class StructureQuarryBakedModel extends BakedModelWrapper<net.minecraft.client.resources.model.BakedModel> {
    private static final ChunkRenderTypeSet BLOCK_RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutoutMipped());
    private static final int ITEM_PIPE_MASK = (1 << Direction.NORTH.ordinal()) | (1 << Direction.SOUTH.ordinal());

    private final CompositeGeometry geometry;
    private final TextureAtlasSprite particleSprite;

    public StructureQuarryBakedModel(
            net.minecraft.client.resources.model.BakedModel base,
            CompositeGeometry geometry,
            TextureAtlasSprite particleSprite) {
        super(base);
        this.geometry = geometry;
        this.particleSprite = particleSprite;
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand,
            ModelData modelData,
            @Nullable RenderType renderType) {
        if (!geometry.isBuilt()) {
            return originalModel.getQuads(state, side, rand, modelData, renderType);
        }
        if (state == null) {
            return itemQuads(side);
        }
        int pipeMask = StructureQuarryBlock.effectivePipeMask(state);
        int nodeMask = nodePreviewFromModelData(modelData);
        List<BakedQuad> out = new ArrayList<>();
        geometry.appendForWorld(out, pipeMask, nodeMask);
        return out;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        if (state != null && state.getBlock() instanceof StructureQuarryBlock) {
            int preview = StructureQuarryBlock.effectiveNodePreviewMask(level, pos, state);
            if (preview != 0) {
                return ModelData.of(StructureQuarryModelProperties.NODE_PREVIEW_MASK, preview);
            }
        }
        return ModelData.EMPTY;
    }

    private static int nodePreviewFromModelData(ModelData modelData) {
        if (modelData == null || !modelData.has(StructureQuarryModelProperties.NODE_PREVIEW_MASK)) {
            return 0;
        }
        Integer mask = modelData.get(StructureQuarryModelProperties.NODE_PREVIEW_MASK);
        return mask != null ? mask : 0;
    }

    private List<BakedQuad> itemQuads(@Nullable Direction side) {
        List<BakedQuad> out = new ArrayList<>();
        geometry.appendForWorld(out, ITEM_PIPE_MASK, 0);
        return out;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return particleSprite != null ? particleSprite : originalModel.getParticleIcon(data);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return BLOCK_RENDER_TYPES;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
