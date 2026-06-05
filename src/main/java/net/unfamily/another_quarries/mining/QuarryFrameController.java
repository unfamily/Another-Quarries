package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.block.structure.StructureQuarryBlock;
import net.unfamily.another_quarries.block.structure.StructureQuarryVisualRefresh;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.registry.ModBlocks;
import net.unfamily.another_quarries.util.QuarryAreaLogic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds and maintains the decorative {@code structure_quarry} frame on the outer volume border.
 */
public final class QuarryFrameController {
    public enum Phase {
        VALIDATING,
        CLEARING,
        PLACING,
        READY
    }

    private Phase phase = Phase.VALIDATING;
    private int frameSignature;
    private boolean skipped;
    private final List<BlockPos> clearQueue = new ArrayList<>();
    private final List<BlockPos> placeQueue = new ArrayList<>();
    private int validationCooldown;

    public Phase getPhase() {
        return phase;
    }

    public boolean isReady() {
        return skipped || phase == Phase.READY;
    }

    public boolean isFrameWorkActive() {
        return !skipped && phase != Phase.READY;
    }

    public List<BlockPos> getClearQueue() {
        return clearQueue;
    }

    public List<BlockPos> getPlaceQueue() {
        return placeQueue;
    }

    public void ensureSignature(int signature) {
        if (frameSignature != signature) {
            frameSignature = signature;
            requestValidation();
        }
    }

    public void requestValidation() {
        phase = Phase.VALIDATING;
        clearQueue.clear();
        placeQueue.clear();
        validationCooldown = 0;
    }

    public void requestFullReboot() {
        requestValidation();
    }

    public void onPowerEnabled() {
        requestValidation();
    }

    public boolean shouldScanBorder() {
        if (skipped) {
            return false;
        }
        if (phase != Phase.READY) {
            return phase == Phase.VALIDATING;
        }
        if (validationCooldown > 0) {
            validationCooldown--;
            return false;
        }
        validationCooldown = ModConfig.frameValidationIntervalTicks();
        return true;
    }

    public void scanBorder(ServerLevel level, QuarryBlockEntity quarry, Direction facing) {
        if (!QuarryAreaLogic.hasFrameOutline(
                quarry.getSizeLeft(), quarry.getSizeRight(), quarry.getSizeHeight(), quarry.getSizeDepth())) {
            skipped = true;
            phase = Phase.READY;
            clearQueue.clear();
            placeQueue.clear();
            return;
        }

        skipped = false;
        clearQueue.clear();
        placeQueue.clear();

        int maxMiningLevel = QuarryDrillAssigner.resolveDrill(quarry.getEquipmentHandler()).maxMiningLevel();
        List<BlockPos> validFrame = QuarryAreaLogic.enumerateFrameStructureBlocks(
                quarry.getBlockPos(),
                facing,
                quarry.getSizeLeft(),
                quarry.getSizeRight(),
                quarry.getSizeHeight(),
                quarry.getSizeDepth());
        Set<BlockPos> validFrameSet = new HashSet<>(validFrame);

        for (BlockPos pos : validFrame) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.STRUCTURE_QUARRY.get())) {
                continue;
            }
            if (!state.isAir()) {
                if (QuarryMiningFilters.isMineable(level, pos, maxMiningLevel)) {
                    clearQueue.add(pos);
                }
                continue;
            }
            placeQueue.add(pos);
        }

        for (BlockPos pos : QuarryAreaLogic.enumerateOuterVolumeBlocks(
                quarry.getBlockPos(),
                facing,
                quarry.getSizeLeft(),
                quarry.getSizeRight(),
                quarry.getSizeHeight(),
                quarry.getSizeDepth())) {
            if (validFrameSet.contains(pos)) {
                continue;
            }
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (level.getBlockState(pos).is(ModBlocks.STRUCTURE_QUARRY.get())) {
                clearQueue.add(pos);
            }
        }

        advancePhaseFromQueues();
    }

    private void advancePhaseFromQueues() {
        if (!clearQueue.isEmpty()) {
            phase = Phase.CLEARING;
        } else if (!placeQueue.isEmpty()) {
            phase = Phase.PLACING;
        } else {
            phase = Phase.READY;
        }
    }

    public void onClearFinished(ServerLevel level, QuarryBlockEntity quarry, Direction facing) {
        clearQueue.clear();
        scanPlaceNeeds(level, quarry, facing);
        advancePhaseFromQueues();
    }

    public void onPlaceFinished() {
        if (placeQueue.isEmpty()) {
            phase = Phase.READY;
        }
    }

    private void scanPlaceNeeds(ServerLevel level, QuarryBlockEntity quarry, Direction facing) {
        placeQueue.clear();
        for (BlockPos pos : QuarryAreaLogic.enumerateFrameStructureBlocks(
                quarry.getBlockPos(),
                facing,
                quarry.getSizeLeft(),
                quarry.getSizeRight(),
                quarry.getSizeHeight(),
                quarry.getSizeDepth())) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.STRUCTURE_QUARRY.get()) && state.isAir()) {
                placeQueue.add(pos);
            }
        }
    }

    public BlockPos takeNextClearTarget(Level level, Set<BlockPos> reserved) {
        int maxMiningLevel = QuarryMiningLevels.TIER_MODDED;
        for (BlockPos pos : clearQueue) {
            if (reserved.contains(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (state.is(ModBlocks.STRUCTURE_QUARRY.get())) {
                return pos;
            }
            if (QuarryMiningFilters.isMineable(level, pos, maxMiningLevel)) {
                return pos;
            }
        }
        return null;
    }

    public BlockPos takeNextPlaceTarget(Level level, Set<BlockPos> reserved) {
        for (BlockPos pos : placeQueue) {
            if (reserved.contains(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return pos;
            }
        }
        return null;
    }

    public boolean removeFrameBlock(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(ModBlocks.STRUCTURE_QUARRY.get())) {
            return false;
        }
        if (!level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL)) {
            return false;
        }
        StructureQuarryBlock.updateConnectionsAround(level, pos);
        StructureQuarryVisualRefresh.refreshAround(level, pos);
        return true;
    }

    public boolean placeFrameBlock(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!current.isAir()) {
            return false;
        }
        BlockState placed = ModBlocks.STRUCTURE_QUARRY.get().defaultBlockState();
        if (!level.setBlock(pos, placed, net.minecraft.world.level.block.Block.UPDATE_ALL)) {
            return false;
        }
        StructureQuarryBlock.updateConnectionsAround(level, pos);
        StructureQuarryVisualRefresh.refreshAround(level, pos);
        return true;
    }

    public void pruneCompletedClearTargets(Level level) {
        clearQueue.removeIf(pos -> {
            BlockState state = level.getBlockState(pos);
            return state.isAir();
        });
    }

    public void pruneCompletedPlaceTargets(Level level) {
        placeQueue.removeIf(pos -> level.getBlockState(pos).is(ModBlocks.STRUCTURE_QUARRY.get()));
    }

    public void save(ValueOutput output) {
        output.putString("FramePhase", phase.name());
        output.putInt("FrameSignature", frameSignature);
        output.putBoolean("FrameSkipped", skipped);
        output.putInt("FrameValidationCooldown", validationCooldown);
    }

    public void load(ValueInput input) {
        try {
            phase = Phase.valueOf(input.getStringOr("FramePhase", Phase.VALIDATING.name()));
        } catch (IllegalArgumentException ignored) {
            phase = Phase.VALIDATING;
        }
        frameSignature = input.getIntOr("FrameSignature", 0);
        skipped = input.getBooleanOr("FrameSkipped", false);
        validationCooldown = input.getIntOr("FrameValidationCooldown", 0);
        clearQueue.clear();
        placeQueue.clear();
    }
}
