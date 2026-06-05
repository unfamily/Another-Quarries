package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.another_quarries.block.QuarryBlock;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.item.QuarryEquipmentSlots;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.util.QuarryDiggingMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class QuarryMiningEngine {
    private final QuarryBlockEntity quarry;
    private final QuarryFrameController frameController = new QuarryFrameController();
    private QuarryBlockQueue queue = QuarryBlockQueue.empty();
    private final List<WorkerState> workers = new ArrayList<>();
    private boolean queueBuilt;
    private int queueSignature;
    private int activeChunkIndex;
    private QuarryBlockQueue.Phase miningPhase = QuarryBlockQueue.Phase.CLEAR_VOLUME;
    private int volumeDy = -1;
    private int belowLayer;
    private int layerCursor;
    private int regenScanCooldown;

    public QuarryMiningEngine(QuarryBlockEntity quarry) {
        this.quarry = quarry;
        this.regenScanCooldown = ModConfig.regenScanIntervalTicks();
    }

    public boolean tick(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        ItemStackHandler equipment = quarry.getEquipmentHandler();
        List<QuarryDrillType> drills = QuarryDrillAssigner.assign(equipment);
        if (drills.isEmpty()) {
            workers.clear();
            setQuarryVisual(level, QuarryBlock.QuarryState.OFF);
            return false;
        }

        BlockState state = level.getBlockState(quarry.getBlockPos());
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        int signature = hashSignature(facing);

        frameController.ensureSignature(signature);
        if (frameController.shouldScanBorder()) {
            frameController.scanBorder(serverLevel, quarry, facing);
        }

        syncWorkers(drills.size());

        ItemStackHandler buffer = quarry.getBufferHandler();
        if (!QuarryOutputHandler.hasBufferSpace(buffer)) {
            setQuarryVisual(level, frameController.isFrameWorkActive()
                    ? QuarryBlock.QuarryState.BOOT
                    : QuarryBlock.QuarryState.OFF);
            syncQueueState();
            return frameController.isFrameWorkActive();
        }

        if (!frameController.isReady()) {
            return tickFrameWork(serverLevel, facing, drills, equipment, buffer);
        }

        return tickMining(serverLevel, facing, drills, equipment, buffer);
    }

    private boolean tickFrameWork(
            ServerLevel level,
            Direction facing,
            List<QuarryDrillType> drills,
            ItemStackHandler equipment,
            ItemStackHandler buffer) {
        boolean progressed = false;
        int rfPerBlock = quarry.estimatedRfPerBlock();

        if (frameController.getPhase() == QuarryFrameController.Phase.CLEARING) {
            progressed = tickFrameClear(level, drills, equipment, buffer, rfPerBlock);
            frameController.pruneCompletedClearTargets(level);
            if (frameController.getClearQueue().isEmpty()) {
                frameController.onClearFinished(level, quarry, facing);
            }
        } else if (frameController.getPhase() == QuarryFrameController.Phase.PLACING) {
            progressed = tickFramePlace(level);
            frameController.pruneCompletedPlaceTargets(level);
            if (frameController.getPlaceQueue().isEmpty()) {
                frameController.onPlaceFinished();
            }
        }

        boolean active = progressed || workers.stream().anyMatch(w -> w.target != null)
                || frameController.isFrameWorkActive();
        if (frameController.isReady()) {
            resetWorkerTargets();
        } else {
            setQuarryVisual(level, QuarryBlock.QuarryState.BOOT);
        }
        return active && frameController.isFrameWorkActive();
    }

    private boolean tickFrameClear(
            ServerLevel level,
            List<QuarryDrillType> drills,
            ItemStackHandler equipment,
            ItemStackHandler buffer,
            int rfPerBlock) {
        boolean progressed = false;

        for (WorkerState worker : workers) {
            if (worker.drillIndex >= drills.size()) {
                continue;
            }
            QuarryDrillType drill = drills.get(worker.drillIndex);
            QuarryWorkContext ctx = buildContext(equipment, drill, rfPerBlock, level);

            if (worker.target == null || !QuarryBlockBreaker.canBreak(level, worker.target, drill)) {
                worker.target = takeNextFrameClearTarget(level, worker);
                worker.progress = 0;
                worker.requiredTicks = worker.target != null
                        ? QuarryBlockBreaker.breakTicksForBlock(level, worker.target, ctx)
                        : 0;
            }

            if (worker.target == null) {
                continue;
            }

            worker.progress++;
            if (worker.progress < worker.requiredTicks) {
                progressed = true;
                continue;
            }

            if (quarry.getEnergyStorage().getEnergyStored() < rfPerBlock) {
                worker.progress = worker.requiredTicks - 1;
                progressed = true;
                continue;
            }

            if (!QuarryOutputHandler.hasBufferSpace(buffer)) {
                worker.progress = worker.requiredTicks - 1;
                progressed = true;
                continue;
            }

            if (QuarryBlockBreaker.breakBlock(level, worker.target, ctx, buffer)) {
                quarry.getEnergyStorage().extractEnergy(rfPerBlock, false);
                quarry.setChanged();
                worker.target = null;
                worker.progress = 0;
                progressed = true;
            } else {
                worker.target = takeNextFrameClearTarget(level, worker);
                worker.progress = 0;
                worker.requiredTicks = worker.target != null
                        ? QuarryBlockBreaker.breakTicksForBlock(level, worker.target, ctx)
                        : 0;
            }
        }

        return progressed;
    }

    private boolean tickFramePlace(ServerLevel level) {
        boolean progressed = false;

        for (WorkerState worker : workers) {
            if (worker.target == null || !level.getBlockState(worker.target).isAir()) {
                worker.target = takeNextFramePlaceTarget(level, worker);
                worker.progress = 0;
                worker.requiredTicks = worker.target != null ? 1 : 0;
            }

            if (worker.target == null) {
                continue;
            }

            worker.progress++;
            if (worker.progress < worker.requiredTicks) {
                progressed = true;
                continue;
            }

            if (frameController.placeFrameBlock(level, worker.target)) {
                quarry.setChanged();
                worker.target = null;
                worker.progress = 0;
                progressed = true;
            } else {
                worker.target = takeNextFramePlaceTarget(level, worker);
                worker.progress = 0;
                worker.requiredTicks = worker.target != null ? 1 : 0;
            }
        }

        return progressed;
    }

    private BlockPos takeNextFrameClearTarget(Level level, WorkerState forWorker) {
        Set<BlockPos> reserved = new HashSet<>();
        for (WorkerState worker : workers) {
            if (worker != forWorker && worker.target != null) {
                reserved.add(worker.target);
            }
        }
        return frameController.takeNextClearTarget(level, reserved);
    }

    private BlockPos takeNextFramePlaceTarget(Level level, WorkerState forWorker) {
        Set<BlockPos> reserved = new HashSet<>();
        for (WorkerState worker : workers) {
            if (worker != forWorker && worker.target != null) {
                reserved.add(worker.target);
            }
        }
        return frameController.takeNextPlaceTarget(level, reserved);
    }

    private boolean tickMining(
            ServerLevel level,
            Direction facing,
            List<QuarryDrillType> drills,
            ItemStackHandler equipment,
            ItemStackHandler buffer) {
        ensureQueue(level);
        if (queueBuilt) {
            queue.fastSkipAir(level);
        }
        scanRegeneratedBlocksIfDue(level);

        boolean progressed = false;
        int rfPerBlock = quarry.estimatedRfPerBlock();

        for (WorkerState worker : workers) {
            if (worker.drillIndex >= drills.size()) {
                continue;
            }
            QuarryDrillType drill = drills.get(worker.drillIndex);
            QuarryWorkContext ctx = buildContext(equipment, drill, rfPerBlock, level);

            if (worker.target == null || !QuarryBlockBreaker.canBreak(level, worker.target, drill)) {
                worker.target = takeNextTarget(level, worker, drill);
                worker.progress = 0;
                worker.requiredTicks = worker.target != null
                        ? QuarryBlockBreaker.breakTicksForBlock(level, worker.target, ctx)
                        : 0;
            }

            if (worker.target == null) {
                continue;
            }

            worker.progress++;
            if (worker.progress < worker.requiredTicks) {
                progressed = true;
                continue;
            }

            if (quarry.getEnergyStorage().getEnergyStored() < rfPerBlock) {
                worker.progress = worker.requiredTicks - 1;
                progressed = true;
                continue;
            }

            if (!QuarryOutputHandler.hasBufferSpace(buffer)) {
                worker.progress = worker.requiredTicks - 1;
                progressed = true;
                continue;
            }

            if (QuarryBlockBreaker.breakBlock(level, worker.target, ctx, buffer)) {
                quarry.getEnergyStorage().extractEnergy(rfPerBlock, false);
                quarry.setChanged();
                worker.target = null;
                worker.progress = 0;
                progressed = true;
            } else {
                worker.target = takeNextTarget(level, worker, drill);
                worker.progress = 0;
                worker.requiredTicks = worker.target != null
                        ? QuarryBlockBreaker.breakTicksForBlock(level, worker.target, ctx)
                        : 0;
            }
        }

        boolean active = progressed || workers.stream().anyMatch(w -> w.target != null);
        setQuarryVisual(level, active ? QuarryBlock.QuarryState.ON : QuarryBlock.QuarryState.OFF);
        syncQueueState();
        return active;
    }

    public void onPowerEnabled() {
        frameController.onPowerEnabled();
        resetWorkerTargets();
    }

    public void invalidateFrame() {
        frameController.requestValidation();
        resetWorkerTargets();
    }

    public void requestFullReboot() {
        invalidateQueue();
        frameController.requestFullReboot();
    }

    public void setIdleVisual(Level level) {
        setQuarryVisual(level, QuarryBlock.QuarryState.OFF);
    }

    public List<BlockPos> getChunkTicketPositions(Level level) {
        BlockState state = level.getBlockState(quarry.getBlockPos());
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

        List<BlockPos> positions = new ArrayList<>(getActiveTargetPositions());
        positions.addAll(frameController.getFramePositionsForChunks(quarry, facing));
        return positions;
    }

    private void syncQueueState() {
        if (queueBuilt) {
            miningPhase = queue.getPhase();
            volumeDy = queue.getVolumeDy();
            belowLayer = queue.getBelowLayer();
            layerCursor = queue.getCursor();
            activeChunkIndex = queue.getChunkIndex();
        }
    }

    private QuarryWorkContext buildContext(ItemStackHandler equipment, QuarryDrillType drill, int rfPerBlock, Level level) {
        BlockState state = level.getBlockState(quarry.getBlockPos());
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        return new QuarryWorkContext(
                drill,
                QuarryEquipmentSlots.diggerModuleCount(equipment),
                QuarryEquipmentSlots.speedModuleCount(equipment),
                QuarryEquipmentSlots.fortuneLevel(equipment),
                QuarryEquipmentSlots.hasSilkTouch(equipment),
                rfPerBlock,
                facing,
                quarry.getDiggingMode());
    }

    private void ensureQueue(Level level) {
        BlockState state = level.getBlockState(quarry.getBlockPos());
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        int signature = hashSignature(facing);

        if (queueBuilt && signature == queueSignature) {
            int maxMiningLevel = QuarryDrillAssigner.resolveDrill(quarry.getEquipmentHandler()).maxMiningLevel();
            if (quarry.getDiggingMode() == QuarryDiggingMode.CHUNK
                    && queue.isCurrentChunkComplete(level, maxMiningLevel)
                    && queue.hasRemainingChunks()) {
                queue.advanceToNextChunk();
                activeChunkIndex = queue.getChunkIndex();
                resetWorkerTargets();
            }
            return;
        }

        if (signature != queueSignature) {
            activeChunkIndex = 0;
            miningPhase = QuarryBlockQueue.Phase.CLEAR_VOLUME;
            volumeDy = -1;
            belowLayer = 0;
            layerCursor = 0;
        }

        List<BlockPos> pendingRegen = queueBuilt && signature == queueSignature
                ? new ArrayList<>(queue.getRegenQueue())
                : List.of();
        int dy = volumeDy >= 0 ? volumeDy : quarry.getSizeHeight();
        queue = QuarryBlockQueue.build(
                quarry.getBlockPos(),
                facing,
                quarry.getSizeLeft(),
                quarry.getSizeRight(),
                quarry.getSizeHeight(),
                quarry.getSizeDepth(),
                quarry.getDiggingMode(),
                activeChunkIndex,
                miningPhase,
                dy,
                belowLayer,
                layerCursor);
        queue.setRegenQueue(pendingRegen);
        queueBuilt = true;
        queueSignature = signature;
        resetWorkerTargets();
    }

    private BlockPos takeNextTarget(Level level, WorkerState forWorker, QuarryDrillType drill) {
        Set<BlockPos> reserved = new HashSet<>();
        for (WorkerState worker : workers) {
            if (worker != forWorker && worker.target != null) {
                reserved.add(worker.target);
            }
        }
        return queue.takeNextMineable(level, reserved, drill.maxMiningLevel());
    }

    private void scanRegeneratedBlocksIfDue(Level level) {
        if (!queueBuilt) {
            return;
        }
        if (--regenScanCooldown > 0) {
            return;
        }
        regenScanCooldown = quarry.getDiggingMode() == QuarryDiggingMode.CHUNK
                ? ModConfig.regenScanIntervalTicks() * 2
                : ModConfig.regenScanIntervalTicks();
        int maxMiningLevel = QuarryDrillAssigner.resolveDrill(quarry.getEquipmentHandler()).maxMiningLevel();
        queue.setRegenQueue(queue.findRegeneratedBlocks(level, maxMiningLevel));
    }

    private void resetWorkerTargets() {
        for (WorkerState worker : workers) {
            worker.target = null;
            worker.progress = 0;
            worker.requiredTicks = 0;
        }
    }

    private int hashSignature(Direction facing) {
        return facing.get2DDataValue()
                ^ (quarry.getSizeLeft() << 4)
                ^ (quarry.getSizeRight() << 8)
                ^ (quarry.getSizeHeight() << 12)
                ^ (quarry.getSizeDepth() << 16)
                ^ (quarry.getDiggingMode().getId() << 20);
    }

    private void syncWorkers(int count) {
        while (workers.size() < count) {
            workers.add(new WorkerState(workers.size()));
        }
        while (workers.size() > count) {
            workers.remove(workers.size() - 1);
        }
    }

    public void invalidateQueue() {
        queueBuilt = false;
        activeChunkIndex = 0;
        miningPhase = QuarryBlockQueue.Phase.CLEAR_VOLUME;
        volumeDy = -1;
        belowLayer = 0;
        layerCursor = 0;
        regenScanCooldown = ModConfig.regenScanIntervalTicks();
        resetWorkerTargets();
    }

    public java.util.List<BlockPos> getActiveTargetPositions() {
        java.util.List<BlockPos> targets = new java.util.ArrayList<>();
        for (WorkerState worker : workers) {
            if (worker.target != null) {
                targets.add(worker.target);
            }
        }
        return targets;
    }

    public void save(net.minecraft.world.level.storage.ValueOutput output) {
        output.putInt("QueueCursor", queueBuilt ? queue.getCursor() : layerCursor);
        output.putInt("VolumeDy", queueBuilt ? queue.getVolumeDy() : volumeDy);
        output.putInt("QueueSignature", queueSignature);
        output.putBoolean("QueueBuilt", queueBuilt);
        output.putInt("ActiveChunkIndex", activeChunkIndex);
        output.putString("MiningPhase", miningPhase.name());
        output.putInt("BelowLayer", belowLayer);
        output.putInt("RegenScanCooldown", regenScanCooldown);
        frameController.save(output);
        var list = output.list("MiningWorkers", WorkerState.CODEC);
        for (WorkerState worker : workers) {
            list.add(worker);
        }
        if (list.isEmpty()) {
            output.discard("MiningWorkers");
        }
    }

    public void load(net.minecraft.world.level.storage.ValueInput input) {
        queueBuilt = false;
        queueSignature = input.getIntOr("QueueSignature", 0);
        activeChunkIndex = input.getIntOr("ActiveChunkIndex", 0);
        try {
            miningPhase = QuarryBlockQueue.Phase.valueOf(
                    input.getStringOr("MiningPhase", QuarryBlockQueue.Phase.CLEAR_VOLUME.name()));
        } catch (IllegalArgumentException ignored) {
            miningPhase = QuarryBlockQueue.Phase.CLEAR_VOLUME;
        }
        volumeDy = input.getIntOr("VolumeDy", -1);
        belowLayer = input.getIntOr("BelowLayer", 0);
        layerCursor = input.getIntOr("QueueCursor", 0);
        regenScanCooldown = input.getIntOr("RegenScanCooldown", ModConfig.regenScanIntervalTicks());
        frameController.load(input);
        queue = QuarryBlockQueue.empty();
        workers.clear();
        for (WorkerState worker : input.listOrEmpty("MiningWorkers", WorkerState.CODEC)) {
            workers.add(worker);
        }
    }

    private void setQuarryVisual(Level level, QuarryBlock.QuarryState target) {
        BlockState state = level.getBlockState(quarry.getBlockPos());
        if (state.getValue(QuarryBlock.STATE) != target) {
            level.setBlock(quarry.getBlockPos(), state.setValue(QuarryBlock.STATE, target), 3);
        }
    }

    public static final class WorkerState {
        public static final com.mojang.serialization.Codec<WorkerState> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(instance ->
                instance.group(
                        com.mojang.serialization.Codec.INT.fieldOf("DrillIndex").forGetter(w -> w.drillIndex),
                        BlockPos.CODEC.optionalFieldOf("Target").forGetter(w -> java.util.Optional.ofNullable(w.target)),
                        com.mojang.serialization.Codec.INT.fieldOf("Progress").forGetter(w -> w.progress),
                        com.mojang.serialization.Codec.INT.fieldOf("RequiredTicks").forGetter(w -> w.requiredTicks)
                ).apply(instance, (drillIndex, target, progress, requiredTicks) ->
                        new WorkerState(drillIndex, target.orElse(null), progress, requiredTicks)));

        int drillIndex;
        BlockPos target;
        int progress;
        int requiredTicks;

        WorkerState(int drillIndex) {
            this(drillIndex, null, 0, 0);
        }

        WorkerState(int drillIndex, BlockPos target, int progress, int requiredTicks) {
            this.drillIndex = drillIndex;
            this.target = target;
            this.progress = progress;
            this.requiredTicks = requiredTicks;
        }
    }
}
