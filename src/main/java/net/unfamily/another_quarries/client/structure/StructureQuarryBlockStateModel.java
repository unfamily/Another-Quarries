package net.unfamily.another_quarries.client.structure;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.another_quarries.block.structure.StructureQuarryBlock;
import net.unfamily.another_quarries.client.CompositeGeometry;
import org.jspecify.annotations.Nullable;

public final class StructureQuarryBlockStateModel implements BlockStateModel {
    private final CompositeGeometry geometry;
    private final Material.Baked particleMaterial;
    private final BlockStateModel fallback;

    public StructureQuarryBlockStateModel(
            CompositeGeometry geometry,
            Material.Baked particleMaterial,
            BlockStateModel fallback) {
        this.geometry = geometry;
        this.particleMaterial = particleMaterial;
        this.fallback = fallback;
    }

    @Override
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        if (!geometry.isBuilt()) {
            fallback.collectParts(level, pos, state, random, output);
            return;
        }
        int pipeMask = StructureQuarryBlock.effectivePipeMask(state);
        int nodeMask = StructureQuarryBlock.effectiveNodePreviewMask(level, pos, state);
        List<BakedQuad> quads = new ArrayList<>();
        geometry.appendForWorld(quads, pipeMask, nodeMask);
        output.add(toPart(quads));
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        if (!geometry.isBuilt()) {
            fallback.collectParts(random, output);
            return;
        }
        output.add(toPart(new ArrayList<>(geometry.lineAllQuads())));
    }

    @Override
    public Material.Baked particleMaterial() {
        return particleMaterial;
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags() {
        return fallback.materialFlags();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return particleMaterial;
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (!geometry.isBuilt()) {
            return fallback.materialFlags(level, pos, state);
        }
        return toPart(buildQuads(level, pos, state)).materialFlags();
    }

    @Override
    public @Nullable Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        if (!geometry.isBuilt()) {
            return fallback.createGeometryKey(level, pos, state, random);
        }
        int pipeMask = StructureQuarryBlock.effectivePipeMask(state);
        int nodeMask = StructureQuarryBlock.effectiveNodePreviewMask(level, pos, state);
        return new GeometryKey(pipeMask, nodeMask);
    }

    private List<BakedQuad> buildQuads(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        List<BakedQuad> quads = new ArrayList<>();
        geometry.appendForWorld(
                quads,
                StructureQuarryBlock.effectivePipeMask(state),
                StructureQuarryBlock.effectiveNodePreviewMask(level, pos, state));
        return quads;
    }

    private BlockStateModelPart toPart(List<BakedQuad> quads) {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (BakedQuad quad : quads) {
            builder.addUnculledFace(quad);
        }
        return new SimpleModelWrapper(builder.build(), true, particleMaterial);
    }

    private record GeometryKey(int pipeMask, int nodeMask) {}
}
