package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.block.structure.StructureQuarryBreakCascade;
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
 * Validation runs on reboot, power-on, and area resize only.
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
    private final Set<BlockPos> placePending = new HashSet<>();

    private boolean validationActive;
    private int validationIndex;
    private int validationFrameTotal;
    private List<BlockPos> frameValidationList;
    /** Frame positions for post-clear placement; kept until READY. */
    private Set<BlockPos> framePositions;
    private int clearPullIndex;
    private int placePullIndex;

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
        placePending.clear();
        clearPullIndex = 0;
        placePullIndex = 0;
        framePositions = null;
        frameValidationList = null;
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
            placePending.clear();
            resetValidationState();
            framePositions = null;
            frameValidationList = null;
            return;
        }

        skipped = false;
        ensureFrameValidationList(quarry, facing);

        int budget = ModConfig.frameValidationBlocksPerTick(validationFrameTotal - validationIndex);
        int maxMiningLevel = QuarryDrillAssigner.resolveDrill(quarry.getEquipmentHandler()).maxMiningLevel();
        int end = Math.min(validationIndex + budget, validationFrameTotal);

        for (int i = validationIndex; i < end; i++) {
            inspectFramePosition(level, frameValidationList.get(i), maxMiningLevel);
        }
        validationIndex = end;

        if (validationIndex >= validationFrameTotal) {
            finishValidation();
        }
    }

    public void onClearQueueDrained() {
        if (phase == Phase.CLEARING) {
            advancePhaseFromQueues();
        }
    }

    public void notifyFrameCleared(Level level, BlockPos pos) {
        if (framePositions == null || !framePositions.contains(pos)) {
            return;
        }
        if (!level.getBlockState(pos).isAir()) {
            return;
        }
        if (placePending.add(pos)) {
            placeQueue.add(pos);
        }
    }

    private void ensureFrameValidationList(QuarryBlockEntity quarry, Direction facing) {
        if (frameValidationList != null) {
            return;
        }
        frameValidationList = QuarryAreaLogic.buildFramePositionList(
                quarry.getBlockPos(),
                facing,
                quarry.getSizeLeft(),
                quarry.getSizeRight(),
                quarry.getSizeHeight(),
                quarry.getSizeDepth());
        validationFrameTotal = frameValidationList.size();
        framePositions = new HashSet<>(frameValidationList);
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
            if (QuarryMiningFilters.isFrameObstruction(level, pos, maxMiningLevel)) {
                clearQueue.add(pos);
            }
            return;
        }
        if (placePending.add(pos)) {
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
            clearPullIndex = 0;
        } else if (!placeQueue.isEmpty()) {
            phase = Phase.PLACING;
            placePullIndex = 0;
        } else {
            phase = Phase.READY;
            framePositions = null;
            frameValidationList = null;
        }
    }

    public void onPlaceFinished() {
        if (phase == Phase.PLACING) {
            phase = Phase.READY;
            framePositions = null;
            frameValidationList = null;
        }
    }

    public boolean isClearWorkComplete(Level level) {
        if (clearQueue.isEmpty()) {
            return true;
        }
        for (int i = clearPullIndex; i < clearQueue.size(); i++) {
            if (needsClearing(level, clearQueue.get(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean isPlaceWorkComplete(Level level) {
        if (placeQueue.isEmpty()) {
            return true;
        }
        for (int i = placePullIndex; i < placeQueue.size(); i++) {
            if (level.getBlockState(placeQueue.get(i)).isAir()) {
                return false;
            }
        }
        return true;
    }

    public BlockPos takeNextClearTarget(Level level, Set<BlockPos> reserved) {
        int maxMiningLevel = QuarryMiningLevels.TIER_MODDED;
        while (clearPullIndex < clearQueue.size()) {
            BlockPos pos = clearQueue.get(clearPullIndex++);
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
        while (placePullIndex < placeQueue.size()) {
            BlockPos pos = placeQueue.get(placePullIndex++);
            if (reserved.contains(pos)) {
                continue;
            }
            if (level.getBlockState(pos).isAir()) {
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
        StructureQuarryBreakCascade.runWithoutCascade(() -> {
            if (!level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL)) {
                return;
            }
            StructureQuarryVisualRefresh.markDirty(pos);
        });
        return level.getBlockState(pos).isAir();
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
        StructureQuarryVisualRefresh.markDirty(pos);
        return true;
    }

    private boolean needsClearing(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.is(ModBlocks.STRUCTURE_QUARRY.get())) {
            return true;
        }
        return QuarryMiningFilters.isMineable(level, pos, QuarryMiningLevels.TIER_MODDED);
    }

    private void resetValidationState() {
        validationActive = false;
        validationIndex = 0;
        validationFrameTotal = 0;
        frameValidationList = null;
    }

    private void beginValidationState() {
        validationActive = true;
        validationIndex = 0;
        validationFrameTotal = 0;
    }

    public void save(CompoundTag tag) {
        tag.putString("FramePhase", phase.name());
        tag.putInt("FrameSignature", frameSignature);
        tag.putBoolean("FrameSkipped", skipped);
    }

    public void load(CompoundTag tag) {
        try {
            phase = Phase.valueOf(tag.contains("FramePhase") ? tag.getString("FramePhase") : Phase.READY.name());
        } catch (IllegalArgumentException ignored) {
            phase = Phase.READY;
        }
        frameSignature = tag.contains("FrameSignature") ? tag.getInt("FrameSignature") : 0;
        skipped = tag.getBoolean("FrameSkipped");
        tag.getInt("FrameValidationCooldown");
        clearQueue.clear();
        placeQueue.clear();
        placePending.clear();
        clearPullIndex = 0;
        placePullIndex = 0;
        resetValidationState();
        framePositions = null;
        frameValidationList = null;
        if (!skipped && phase != Phase.READY) {
            phase = Phase.READY;
        }
    }
}
