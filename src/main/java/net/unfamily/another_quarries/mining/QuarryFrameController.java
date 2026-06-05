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
import java.util.List;
import java.util.Set;

/**
 * Builds and maintains the decorative {@code structure_quarry} frame on the outer volume border.
 * Validation runs on reboot, power-on, and area resize only.
 */
public final class QuarryFrameController {
    public enum Phase {
        VALIDATING,
        CLEARING,
        PLACING,
        READY
    }

    private enum ValidationStep {
        FRAME,
        STRAY
    }

    private Phase phase = Phase.VALIDATING;
    private int frameSignature;
    private boolean skipped;
    private final List<BlockPos> clearQueue = new ArrayList<>();
    private final List<BlockPos> placeQueue = new ArrayList<>();

    private boolean validationActive;
    private ValidationStep validationStep = ValidationStep.FRAME;
    private int validationIndex;
    private Set<BlockPos> validFrameSet;
    private int validationFrameTotal;
    private int validationStrayTotal;

    private boolean placeScanActive;
    private int placeScanIndex;
    private int placeScanTotal;

    public Phase getPhase() {
        return phase;
    }

    public boolean isReady() {
        if (skipped) {
            return true;
        }
        return phase == Phase.READY;
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
        resetPlaceScanState();
        beginValidationState();
    }

    public void requestFullReboot() {
        requestValidation();
    }

    public void onPowerEnabled() {
        if (!isReady()) {
            requestValidation();
        }
    }

    public boolean shouldScanBorder() {
        if (skipped) {
            return false;
        }
        return phase == Phase.VALIDATING && validationActive;
    }

    public void scanBorder(ServerLevel level, QuarryBlockEntity quarry, Direction facing) {
        if (!validationActive) {
            return;
        }

        if (!QuarryAreaLogic.hasFrameOutline(
                quarry.getSizeLeft(), quarry.getSizeRight(), quarry.getSizeHeight(), quarry.getSizeDepth())) {
            skipped = true;
            phase = Phase.READY;
            clearQueue.clear();
            placeQueue.clear();
            resetValidationState();
            resetPlaceScanState();
            return;
        }

        skipped = false;
        ensureValidationTotals(quarry, facing);

        int budget = ModConfig.frameValidationBlocksPerTick();
        int maxMiningLevel = QuarryDrillAssigner.resolveDrill(quarry.getEquipmentHandler()).maxMiningLevel();

        if (validationStep == ValidationStep.FRAME) {
            validationIndex = QuarryAreaLogic.forEachFrameStructureBlockSlice(
                    quarry.getBlockPos(),
                    facing,
                    quarry.getSizeLeft(),
                    quarry.getSizeRight(),
                    quarry.getSizeHeight(),
                    quarry.getSizeDepth(),
                    validationIndex,
                    budget,
                    pos -> inspectFramePosition(level, pos, maxMiningLevel));
            if (validationIndex >= validationFrameTotal) {
                validationStep = ValidationStep.STRAY;
                validationIndex = 0;
            }
            return;
        }

        validationIndex = QuarryAreaLogic.forEachBorderShellBlockSlice(
                quarry.getBlockPos(),
                facing,
                quarry.getSizeLeft(),
                quarry.getSizeRight(),
                quarry.getSizeHeight(),
                quarry.getSizeDepth(),
                validationIndex,
                budget,
                pos -> inspectStrayPosition(level, pos));
        if (validationIndex >= validationStrayTotal) {
            finishValidation();
        }
    }

    public void tickPlaceScanIfNeeded(ServerLevel level, QuarryBlockEntity quarry, Direction facing) {
        if (phase != Phase.CLEARING || !clearQueue.isEmpty()) {
            return;
        }

        if (!placeScanActive) {
            placeScanActive = true;
            placeScanIndex = 0;
            placeScanTotal = QuarryAreaLogic.frameStructureBlockCount(
                    quarry.getBlockPos(),
                    facing,
                    quarry.getSizeLeft(),
                    quarry.getSizeRight(),
                    quarry.getSizeHeight(),
                    quarry.getSizeDepth());
            return;
        }

        int budget = ModConfig.frameValidationBlocksPerTick();
        placeScanIndex = QuarryAreaLogic.forEachFrameStructureBlockSlice(
                quarry.getBlockPos(),
                facing,
                quarry.getSizeLeft(),
                quarry.getSizeRight(),
                quarry.getSizeHeight(),
                quarry.getSizeDepth(),
                placeScanIndex,
                budget,
                pos -> inspectPlacePosition(level, pos));
        if (placeScanIndex >= placeScanTotal) {
            resetPlaceScanState();
            advancePhaseFromQueues();
        }
    }

    private void ensureValidationTotals(QuarryBlockEntity quarry, Direction facing) {
        if (validFrameSet != null) {
            return;
        }
        validFrameSet = QuarryAreaLogic.frameStructureBlockSet(
                quarry.getBlockPos(),
                facing,
                quarry.getSizeLeft(),
                quarry.getSizeRight(),
                quarry.getSizeHeight(),
                quarry.getSizeDepth());
        validationFrameTotal = validFrameSet.size();
        validationStrayTotal = QuarryAreaLogic.borderShellBlockCount(
                quarry.getSizeLeft(),
                quarry.getSizeRight(),
                quarry.getSizeHeight(),
                quarry.getSizeDepth());
    }

    private void inspectFramePosition(ServerLevel level, BlockPos pos, int maxMiningLevel) {
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.STRUCTURE_QUARRY.get())) {
            return;
        }
        if (!state.isAir()) {
            if (QuarryMiningFilters.isMineable(level, pos, maxMiningLevel)) {
                clearQueue.add(pos);
            }
            return;
        }
        placeQueue.add(pos);
    }

    private void inspectStrayPosition(ServerLevel level, BlockPos pos) {
        if (validFrameSet.contains(pos)) {
            return;
        }
        if (!level.isLoaded(pos)) {
            return;
        }
        if (level.getBlockState(pos).is(ModBlocks.STRUCTURE_QUARRY.get())) {
            clearQueue.add(pos);
        }
    }

    private void inspectPlacePosition(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.STRUCTURE_QUARRY.get()) && state.isAir()) {
            placeQueue.add(pos);
        }
    }

    private void finishValidation() {
        resetValidationState();
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

    public void onPlaceFinished() {
        if (placeQueue.isEmpty()) {
            phase = Phase.READY;
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

    private void resetValidationState() {
        validationActive = false;
        validationStep = ValidationStep.FRAME;
        validationIndex = 0;
        validFrameSet = null;
        validationFrameTotal = 0;
        validationStrayTotal = 0;
    }

    private void resetPlaceScanState() {
        placeScanActive = false;
        placeScanIndex = 0;
        placeScanTotal = 0;
    }

    private void beginValidationState() {
        validationActive = true;
        validationStep = ValidationStep.FRAME;
        validationIndex = 0;
        validFrameSet = null;
        validationFrameTotal = 0;
        validationStrayTotal = 0;
    }

    public void save(ValueOutput output) {
        output.putString("FramePhase", phase.name());
        output.putInt("FrameSignature", frameSignature);
        output.putBoolean("FrameSkipped", skipped);
    }

    public void load(ValueInput input) {
        try {
            phase = Phase.valueOf(input.getStringOr("FramePhase", Phase.READY.name()));
        } catch (IllegalArgumentException ignored) {
            phase = Phase.READY;
        }
        frameSignature = input.getIntOr("FrameSignature", 0);
        skipped = input.getBooleanOr("FrameSkipped", false);
        input.getIntOr("FrameValidationCooldown", 0);
        clearQueue.clear();
        placeQueue.clear();
        resetValidationState();
        resetPlaceScanState();
        if (!skipped && phase != Phase.READY) {
            phase = Phase.READY;
        }
    }
}
