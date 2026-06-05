package net.unfamily.another_quarries.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.another_quarries.client.gui.QuarryMenu;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.item.QuarryEquipmentSlots;
import net.unfamily.another_quarries.item.QuarryModules;
import net.unfamily.another_quarries.mining.QuarryChunkTickets;
import net.unfamily.another_quarries.mining.QuarryDrillAssigner;
import net.unfamily.another_quarries.mining.QuarryMiningEngine;
import net.unfamily.another_quarries.mining.QuarryOutputHandler;
import net.unfamily.another_quarries.registry.ModBlockEntities;
import net.unfamily.another_quarries.registry.ModItems;
import net.unfamily.another_quarries.util.QuarryDiggingMode;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jspecify.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class QuarryBlockEntity extends BlockEntity implements MenuProvider {
    public static final int BUFFER_SLOT_COUNT = 27;
    public static int equipmentSlotCount() {
        return QuarryEquipmentSlots.slotCount();
    }

    /** @deprecated use {@link QuarryEquipmentSlots#firstDroneSlot()} */
    @Deprecated
    public static final int DRONE_SLOT_INDEX = 0;
    /** @deprecated use {@link QuarryEquipmentSlots#drillSlotStart()} */
    @Deprecated
    public static final int DRILL_SLOT_INDEX = 1;
    /** @deprecated use {@link QuarryEquipmentSlots#diggerModuleSlot()} */
    @Deprecated
    public static final int MODULE_SLOT_START = 2;
    /** @deprecated only three module slots now */
    @Deprecated
    public static final int MODULE_SLOT_COUNT = 3;

    private static final int MIN_HEIGHT_OR_DEPTH = 0;
    /** Default block count per axis when the quarry is first placed (15×15×15). */
    private static final int DEFAULT_AXIS_BLOCKS = 15;
    private static final int DEFAULT_SIZE_LEFT = (DEFAULT_AXIS_BLOCKS - 1) / 2;
    private static final int DEFAULT_SIZE_RIGHT = DEFAULT_AXIS_BLOCKS - 1 - DEFAULT_SIZE_LEFT;
    private static final int DEFAULT_SIZE_HEIGHT = DEFAULT_AXIS_BLOCKS - 1;
    private static final int DEFAULT_SIZE_DEPTH = DEFAULT_AXIS_BLOCKS - 1;

    private int sizeLeft = DEFAULT_SIZE_LEFT;
    private int sizeRight = DEFAULT_SIZE_RIGHT;
    private int sizeHeight = DEFAULT_SIZE_HEIGHT;
    private int sizeDepth = DEFAULT_SIZE_DEPTH;
    private boolean previewEnabled;
    private int redstoneMode;
    private boolean pulsePreviousRedstone;
    private boolean pulseEdgeAllowsWork;
    private boolean previousCanWork;

    private final ItemStackHandler bufferHandler;
    private final ItemStackHandler equipmentHandler;
    private final EnergyStorageImpl energyStorage;
    private final EnergyHandlerImpl energyHandler;
    private final ResourceHandler<ItemResource> itemTransferHandler;
    private final QuarryMiningEngine miningEngine;
    private final LongOpenHashSet forcedMiningChunks = new LongOpenHashSet();

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUARRY_BE.get(), pos, state);
        this.bufferHandler = new ItemStackHandler(BUFFER_SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.equipmentHandler = new ItemStackHandler(QuarryEquipmentSlots.slotCount()) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                refreshEnergyCapacity();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return QuarryEquipmentSlots.isValid(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return QuarryEquipmentSlots.getSlotLimit(slot, this);
            }
        };
        int initialCapacity = ModConfig.resolveEnergyBufferCapacity(peakRfPerTick());
        this.energyStorage = new EnergyStorageImpl(initialCapacity, initialCapacity, initialCapacity);
        this.energyHandler = new EnergyHandlerImpl();
        this.itemTransferHandler = LegacyItemHandlerResourceHandler.wrap(bufferHandler);
        this.miningEngine = new QuarryMiningEngine(this);
    }

    private int peakRfPerTick() {
        int logicalWorkers = QuarryEquipmentSlots.effectiveWorkerCount(equipmentHandler);
        int activeWorkers = QuarryDrillAssigner.activeWorkerCount(equipmentHandler);
        int blocksPerCompletion = ModConfig.blocksPerWorkerCompletion(logicalWorkers, activeWorkers);
        int blocksPerTick = Math.min(ModConfig.maxBlockBreaksPerTick(), activeWorkers * blocksPerCompletion);
        return blocksPerTick * estimatedRfPerBlock();
    }

    public void refreshEnergyCapacity() {
        int capacity = ModConfig.resolveEnergyBufferCapacity(peakRfPerTick());
        energyStorage.setCapacity(capacity, capacity, capacity);
    }

    public ItemStackHandler getBufferHandler() {
        return bufferHandler;
    }

    public ItemStackHandler getEquipmentHandler() {
        return equipmentHandler;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public net.neoforged.neoforge.transfer.energy.EnergyHandler getEnergyHandler() {
        return energyHandler;
    }

    public IItemHandler getBufferItemHandler() {
        return bufferHandler;
    }

    public ResourceHandler<ItemResource> getItemTransferHandler() {
        return itemTransferHandler;
    }

    public int getEnergyStoredDisplay() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyDisplay() {
        return energyStorage.getCapacity();
    }

    public int getSizeLeft() {
        return sizeLeft;
    }

    public int getSizeRight() {
        return sizeRight;
    }

    public int getSizeHeight() {
        return sizeHeight;
    }

    public int getSizeDepth() {
        return sizeDepth;
    }

    public boolean isPreviewEnabled() {
        return previewEnabled;
    }

    public void setPreviewEnabled(boolean previewEnabled) {
        if (this.previewEnabled != previewEnabled) {
            this.previewEnabled = previewEnabled;
            setChanged();
        }
    }

    public QuarryDiggingMode getDiggingMode() {
        return QuarryDiggingMode.CHUNK;
    }

    public boolean requiresChunkDiggingMode() {
        return true;
    }

    public int getRedstoneMode() {
        return Math.max(0, Math.min(redstoneMode, 4));
    }

    public void setRedstoneMode(int mode) {
        int newMode = Math.max(0, Math.min(mode, 4));
        if (redstoneMode != newMode) {
            redstoneMode = newMode;
            setChanged();
        }
    }

    public void cycleRedstoneMode() {
        setRedstoneMode((getRedstoneMode() + 1) % 5);
    }

    public void cycleRedstoneModeBackward() {
        int mode = getRedstoneMode();
        setRedstoneMode(mode == 0 ? 4 : mode - 1);
    }

    public int getMaxBlockCount() {
        return ModConfig.quarryMaxRange();
    }

    public int getMaxHeight() {
        return Math.max(MIN_HEIGHT_OR_DEPTH, getMaxBlockCount() - 1);
    }

    public int getMaxDepth() {
        return Math.max(MIN_HEIGHT_OR_DEPTH, getMaxBlockCount() - 1);
    }

    public int getMaxWidth() {
        return Math.max(MIN_HEIGHT_OR_DEPTH, getMaxBlockCount() - 1);
    }

    /** Direction: 0=up, 1=left, 2=right, 3=depth. */
    public void adjustSize(int direction, boolean increment, int amount) {
        int delta = increment ? amount : -amount;
        switch (direction) {
            case 0 -> sizeHeight = Math.max(MIN_HEIGHT_OR_DEPTH, Math.min(getMaxHeight(), sizeHeight + delta));
            case 1 -> {
                int newR = Math.max(0, Math.min(getMaxWidth() - sizeLeft, sizeRight + delta));
                sizeRight = newR;
            }
            case 2 -> {
                int newL = Math.max(0, Math.min(getMaxWidth() - sizeRight, sizeLeft + delta));
                sizeLeft = newL;
            }
            case 3 -> sizeDepth = Math.max(MIN_HEIGHT_OR_DEPTH, Math.min(getMaxDepth(), sizeDepth + delta));
            default -> {}
        }
        miningEngine.invalidateQueue();
        miningEngine.invalidateFrame();
        setChanged();
    }

    public void requestFullReboot() {
        miningEngine.requestFullReboot();
        setChanged();
    }

    public int estimatedRfPerBlock() {
        return ModConfig.totalRfPerBlock(
                QuarryEquipmentSlots.diggerModuleCount(equipmentHandler),
                QuarryEquipmentSlots.speedModuleCount(equipmentHandler),
                QuarryEquipmentSlots.fortuneLevel(equipmentHandler),
                QuarryEquipmentSlots.hasSilkTouch(equipmentHandler),
                QuarryDrillAssigner.resolveDrill(equipmentHandler));
    }

    public void updatePulseEdge() {
        pulseEdgeAllowsWork = false;
        if (redstoneMode != 3 || level == null || level.isClientSide()) {
            return;
        }
        boolean sig = level.hasNeighborSignal(worldPosition);
        pulseEdgeAllowsWork = sig && !pulsePreviousRedstone;
        pulsePreviousRedstone = sig;
    }

    public boolean canWork() {
        if (redstoneMode == 3) {
            return pulseEdgeAllowsWork;
        }
        if (level == null) {
            return true;
        }
        boolean sig = level.hasNeighborSignal(worldPosition);
        return switch (redstoneMode) {
            case 0 -> true;
            case 1 -> !sig;
            case 2 -> sig;
            case 4 -> false;
            default -> true;
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, QuarryBlockEntity be) {
        be.updatePulseEdge();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean canWork = be.canWork();
        if (canWork && !be.previousCanWork) {
            be.miningEngine.onPowerEnabled();
        }
        be.previousCanWork = canWork;
        if (!canWork) {
            be.miningEngine.setIdleVisual(level);
            be.releaseMiningChunkTickets(serverLevel);
            return;
        }
        QuarryOutputHandler.tryEjectBufferUp(serverLevel, pos, be.bufferHandler);
        be.miningEngine.tick(level);
        be.updateMiningChunkTickets(serverLevel);
    }

    public void drops() {
        if (level == null || level.isClientSide()) {
            return;
        }
        for (int i = 0; i < bufferHandler.getSlots(); i++) {
            ItemStack stack = bufferHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                bufferHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        for (int i = 0; i < equipmentHandler.getSlots(); i++) {
            ItemStack stack = equipmentHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                equipmentHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        setChanged();
    }

    public ItemStack createDropStack(BlockState state) {
        return new ItemStack(state.getBlock());
    }

    private void updateMiningChunkTickets(ServerLevel serverLevel) {
        QuarryChunkTickets.sync(serverLevel, worldPosition, miningEngine.getChunkTicketPositions(serverLevel), forcedMiningChunks);
    }

    private void releaseMiningChunkTickets(ServerLevel serverLevel) {
        QuarryChunkTickets.releaseAll(serverLevel, worldPosition, forcedMiningChunks);
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            releaseMiningChunkTickets(serverLevel);
        }
        super.setRemoved();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.another_quarries.quarry");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new QuarryMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("SizeLeft", sizeLeft);
        output.putInt("SizeRight", sizeRight);
        output.putInt("SizeHeight", sizeHeight);
        output.putInt("SizeDepth", sizeDepth);
        output.putBoolean("PreviewEnabled", previewEnabled);
        output.putInt("RedstoneMode", redstoneMode);
        output.putInt("Energy", energyStorage.getEnergyStored());
        output.putInt("EquipmentVersion", QuarryEquipmentSlots.EQUIPMENT_LAYOUT_VERSION);
        saveHandlerSlots(output, "Buffer", bufferHandler);
        saveHandlerSlots(output, "Equipment", equipmentHandler);
        miningEngine.save(output);
    }

    private static void saveHandlerSlots(ValueOutput output, String key, ItemStackHandler handler) {
        ValueOutput.TypedOutputList<ItemStackWithSlot> list = output.list(key, ItemStackWithSlot.CODEC);
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                list.add(new ItemStackWithSlot(i, stack));
            }
        }
        if (list.isEmpty()) {
            output.discard(key);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        sizeLeft = input.getIntOr("SizeLeft", DEFAULT_SIZE_LEFT);
        sizeRight = input.getIntOr("SizeRight", DEFAULT_SIZE_RIGHT);
        sizeHeight = input.getIntOr("SizeHeight", DEFAULT_SIZE_HEIGHT);
        sizeDepth = input.getIntOr("SizeDepth", DEFAULT_SIZE_DEPTH);
        previewEnabled = input.getBooleanOr("PreviewEnabled", false);
        input.getIntOr("DiggingMode", 0);
        redstoneMode = input.getIntOr("RedstoneMode", 0);
        energyStorage.setEnergy(input.getIntOr("Energy", 0));
        int equipmentVersion = input.getIntOr("EquipmentVersion", 1);
        if (equipmentVersion < QuarryEquipmentSlots.EQUIPMENT_LAYOUT_VERSION) {
            migrateLegacyEquipment(input, equipmentHandler, equipmentVersion);
        } else {
            loadHandlerSlots(input, "Equipment", equipmentHandler);
        }
        loadHandlerSlots(input, "Buffer", bufferHandler);
        miningEngine.load(input);
        miningEngine.invalidateQueue();
        refreshEnergyCapacity();
    }

    private static void loadHandlerSlots(ValueInput input, String key, ItemStackHandler handler) {
        for (ItemStackWithSlot entry : input.listOrEmpty(key, ItemStackWithSlot.CODEC)) {
            if (entry.slot() >= 0 && entry.slot() < handler.getSlots()) {
                handler.setStackInSlot(entry.slot(), entry.stack());
            }
        }
    }

    private static final int LEGACY_V2_DIGGER_SLOT = 5;
    private static final int LEGACY_V2_SPEED_SLOT = 6;
    private static final int LEGACY_V2_ENCHANT_SLOT = 7;

    private static void migrateLegacyEquipment(ValueInput input, ItemStackHandler handler, int fromVersion) {
        java.util.HashMap<Integer, ItemStack> bySlot = new java.util.HashMap<>();
        for (ItemStackWithSlot entry : input.listOrEmpty("Equipment", ItemStackWithSlot.CODEC)) {
            bySlot.put(entry.slot(), entry.stack());
        }

        for (int i = 0; i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, ItemStack.EMPTY);
        }

        if (fromVersion == 3) {
            migrateFromLayoutV3(handler, bySlot);
            return;
        }

        mergeStack(handler, QuarryEquipmentSlots.firstDroneSlot(),
                bySlot.getOrDefault(QuarryEquipmentSlots.legacyV3DroneSlot(), ItemStack.EMPTY));

        ItemStack bestDrill = pickBestLegacyDrill(bySlot, fromVersion);
        if (!bestDrill.isEmpty()) {
            handler.setStackInSlot(QuarryEquipmentSlots.drillSlotStart(), bestDrill);
        }

        migrateLegacyModules(handler, bySlot, fromVersion);
    }

    private static void migrateFromLayoutV3(ItemStackHandler handler, java.util.Map<Integer, ItemStack> bySlot) {
        mergeStack(handler, QuarryEquipmentSlots.firstDroneSlot(),
                bySlot.getOrDefault(QuarryEquipmentSlots.legacyV3DroneSlot(), ItemStack.EMPTY));

        ItemStack drill = bySlot.get(QuarryEquipmentSlots.legacyV3DrillSlot());
        if (drill != null && !drill.isEmpty()) {
            handler.setStackInSlot(QuarryEquipmentSlots.drillSlotStart(), singleDrillStack(normalizeLegacyDrill(drill)));
        }

        migrateLegacyModulesFromV3(handler, bySlot);
    }

    private static void migrateLegacyModulesFromV3(ItemStackHandler handler, java.util.Map<Integer, ItemStack> bySlot) {
        int[] diggerCount = {0};
        int[] speedCount = {0};
        ItemStack[] fortuneStack = {ItemStack.EMPTY};
        ItemStack[] silkStack = {ItemStack.EMPTY};

        accumulateModules(bySlot.get(QuarryEquipmentSlots.legacyV3DiggerSlot()), diggerCount, speedCount, fortuneStack, silkStack);
        accumulateModules(bySlot.get(QuarryEquipmentSlots.legacyV3SpeedSlot()), diggerCount, speedCount, fortuneStack, silkStack);
        accumulateModules(bySlot.get(QuarryEquipmentSlots.legacyV3EnchantSlot()), diggerCount, speedCount, fortuneStack, silkStack);

        applyConsolidatedModules(handler, diggerCount[0], speedCount[0], fortuneStack[0], silkStack[0]);
    }

    private static ItemStack pickBestLegacyDrill(java.util.Map<Integer, ItemStack> bySlot, int fromVersion) {
        ItemStack netherite = ItemStack.EMPTY;
        ItemStack laser = ItemStack.EMPTY;
        ItemStack diamond = ItemStack.EMPTY;

        java.util.List<ItemStack> candidates = new java.util.ArrayList<>();
        if (fromVersion >= 2) {
            for (int slot = 1; slot <= 4; slot++) {
                ItemStack stack = bySlot.get(slot);
                if (stack != null && !stack.isEmpty()) {
                    candidates.add(stack);
                }
            }
        } else {
            ItemStack slotOne = bySlot.get(QuarryEquipmentSlots.legacyV3DrillSlot());
            if (slotOne != null && !slotOne.isEmpty()) {
                candidates.add(slotOne);
            }
            for (var entry : bySlot.entrySet()) {
                if (entry.getKey() > QuarryEquipmentSlots.legacyV3DrillSlot() && !normalizeLegacyDrill(entry.getValue()).isEmpty()) {
                    candidates.add(entry.getValue());
                }
            }
        }

        for (ItemStack raw : candidates) {
            ItemStack stack = normalizeLegacyDrill(raw);
            if (stack.isEmpty()) {
                continue;
            }
            if (ModItems.isNetheriteDrill(stack)) {
                netherite = stack;
            } else if (ModItems.isDrillLaser(stack)) {
                laser = stack;
            } else if (ModItems.isDiamondDrill(stack)) {
                diamond = stack;
            }
        }

        if (!netherite.isEmpty()) {
            return singleDrillStack(netherite);
        }
        if (!laser.isEmpty()) {
            return singleDrillStack(laser);
        }
        if (!diamond.isEmpty()) {
            return singleDrillStack(diamond);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack normalizeLegacyDrill(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (ModItems.isAnyDrill(stack) || ModItems.isDrillLaser(stack)) {
            return stack.copy();
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if ("another_quarries".equals(id.getNamespace()) && "drone_laser".equals(id.getPath())) {
            return new ItemStack(ModItems.DRILL_LASER.get(), stack.getCount());
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack singleDrillStack(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static void migrateLegacyModules(ItemStackHandler handler, java.util.Map<Integer, ItemStack> bySlot, int fromVersion) {
        int[] diggerCount = {0};
        int[] speedCount = {0};
        ItemStack[] fortuneStack = {ItemStack.EMPTY};
        ItemStack[] silkStack = {ItemStack.EMPTY};

        if (fromVersion >= 2) {
            accumulateModules(bySlot.get(LEGACY_V2_DIGGER_SLOT), diggerCount, speedCount, fortuneStack, silkStack);
            accumulateModules(bySlot.get(LEGACY_V2_SPEED_SLOT), diggerCount, speedCount, fortuneStack, silkStack);
            accumulateModules(bySlot.get(LEGACY_V2_ENCHANT_SLOT), diggerCount, speedCount, fortuneStack, silkStack);
        } else {
            for (var entry : bySlot.entrySet()) {
                if (entry.getKey() <= QuarryEquipmentSlots.legacyV3DrillSlot()) {
                    continue;
                }
                accumulateModules(entry.getValue(), diggerCount, speedCount, fortuneStack, silkStack);
            }
        }

        applyConsolidatedModules(handler, diggerCount[0], speedCount[0], fortuneStack[0], silkStack[0]);
    }

    private static void applyConsolidatedModules(
            ItemStackHandler handler,
            int diggerCount,
            int speedCount,
            ItemStack fortuneStack,
            ItemStack silkStack) {
        if (diggerCount > 0) {
            handler.setStackInSlot(QuarryEquipmentSlots.diggerModuleSlot(),
                    new ItemStack(ModItems.MODULE_DIGGER.get(), Math.min(diggerCount, ModConfig.maxDiggerModules())));
        }
        if (speedCount > 0) {
            handler.setStackInSlot(QuarryEquipmentSlots.speedModuleSlot(),
                    new ItemStack(ModItems.MODULE_SPEED.get(), Math.min(speedCount, ModConfig.maxSpeedModules())));
        }
        if (!silkStack.isEmpty()) {
            handler.setStackInSlot(QuarryEquipmentSlots.enchantModuleSlot(),
                    new ItemStack(ModItems.MODULE_SILK_TOUCH.get(),
                            Math.min(silkStack.getCount(), ModConfig.maxSilkTouchModules())));
        } else if (!fortuneStack.isEmpty()) {
            handler.setStackInSlot(QuarryEquipmentSlots.enchantModuleSlot(),
                    new ItemStack(ModItems.MODULE_FORTUNE.get(),
                            Math.min(fortuneStack.getCount(), ModConfig.maxFortuneModules())));
        }
    }

    private static void accumulateModules(ItemStack stack, int[] diggerCount, int[] speedCount,
            ItemStack[] fortuneStack, ItemStack[] silkStack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (stack.is(ModItems.MODULE_DIGGER.get())) {
            diggerCount[0] += stack.getCount();
        } else if (stack.is(ModItems.MODULE_SPEED.get())) {
            speedCount[0] += stack.getCount();
        } else if (stack.is(ModItems.MODULE_FORTUNE.get())) {
            fortuneStack[0] = mergeItems(fortuneStack[0], stack);
        } else if (stack.is(ModItems.MODULE_SILK_TOUCH.get())) {
            silkStack[0] = mergeItems(silkStack[0], stack);
        }
    }

    private static void mergeStack(ItemStackHandler handler, int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack existing = handler.getStackInSlot(slot);
        if (existing.isEmpty()) {
            handler.setStackInSlot(slot, stack.copy());
        } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
            existing.grow(stack.getCount());
        }
    }

    private static ItemStack mergeItems(ItemStack a, ItemStack b) {
        if (a.isEmpty()) {
            return b.copy();
        }
        if (ItemStack.isSameItemSameComponents(a, b)) {
            a.grow(b.getCount());
        }
        return a;
    }

    public static class EnergyStorageImpl extends EnergyStorage {
        public EnergyStorageImpl(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity, int maxReceive, int maxExtract) {
            this.capacity = Math.max(1, capacity);
            this.maxReceive = Math.max(0, maxReceive);
            this.maxExtract = Math.max(0, maxExtract);
            if (energy > this.capacity) {
                energy = this.capacity;
            }
        }

        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }
    }

    private final class EnergyHandlerImpl extends net.neoforged.neoforge.transfer.transaction.SnapshotJournal<Integer>
            implements net.neoforged.neoforge.transfer.energy.EnergyHandler {
        @Override
        protected Integer createSnapshot() {
            return energyStorage.getEnergyStored();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            energyStorage.setEnergy(snapshot);
        }

        @Override
        public long getAmountAsLong() {
            return energyStorage.getEnergyStored();
        }

        @Override
        public long getCapacityAsLong() {
            return energyStorage.getCapacity();
        }

        @Override
        public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            net.neoforged.neoforge.transfer.TransferPreconditions.checkNonNegative(amount);
            if (amount == 0 || energyStorage.getCapacity() <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            return energyStorage.receiveEnergy(amount, false);
        }

        @Override
        public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            net.neoforged.neoforge.transfer.TransferPreconditions.checkNonNegative(amount);
            if (amount == 0 || energyStorage.getCapacity() <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            return energyStorage.extractEnergy(amount, false);
        }
    }
}
