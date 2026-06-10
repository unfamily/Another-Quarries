package net.unfamily.another_quarries.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.another_quarries.client.gui.QuarryMenu;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.item.QuarryEquipmentSlots;
import net.unfamily.another_quarries.item.QuarryFilterModuleData;
import net.unfamily.another_quarries.item.QuarryModules;
import net.unfamily.another_quarries.mining.QuarryChunkTickets;
import net.unfamily.another_quarries.mining.QuarryDrillAssigner;
import net.unfamily.another_quarries.mining.QuarryMiningEngine;
import net.unfamily.another_quarries.mining.QuarryOutputHandler;
import net.unfamily.another_quarries.registry.ModBlockEntities;
import net.unfamily.another_quarries.registry.ModItems;
import net.unfamily.another_quarries.util.QuarryAreaLogic;
import net.unfamily.another_quarries.util.QuarryDiggingMode;
import net.unfamily.another_quarries.util.QuarryRedstoneUtil;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.List;

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
    /** Default block count per axis when the quarry is first placed (15×15 footprint, height 5). */
    private static final int DEFAULT_AXIS_BLOCKS = 15;
    private static final int DEFAULT_SIZE_LEFT = (DEFAULT_AXIS_BLOCKS - 1) / 2;
    private static final int DEFAULT_SIZE_RIGHT = DEFAULT_AXIS_BLOCKS - 1 - DEFAULT_SIZE_LEFT;
    private static final int DEFAULT_SIZE_HEIGHT = 4;
    private static final int DEFAULT_SIZE_DEPTH = DEFAULT_AXIS_BLOCKS - 1;

    private int sizeLeft = DEFAULT_SIZE_LEFT;
    private int sizeRight = DEFAULT_SIZE_RIGHT;
    private int sizeHeight = DEFAULT_SIZE_HEIGHT;
    private int sizeDepth = DEFAULT_SIZE_DEPTH;
    private boolean previewEnabled;
    private int redstoneMode;
    private QuarryDiggingMode diggingMode = QuarryDiggingMode.VOLUME;
    private boolean previousCanWork;

    private final QuarryBufferHandler bufferHandler;
    private final ItemStackHandler equipmentHandler;
    private final EnergyStorageImpl energyStorage;
    private final QuarryMiningEngine miningEngine;
    private final LongOpenHashSet forcedMiningChunks = new LongOpenHashSet();

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUARRY_BE.get(), pos, state);
        this.bufferHandler = new QuarryBufferHandler(BUFFER_SLOT_COUNT) {
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
                if (slot == QuarryEquipmentSlots.filterModuleSlot()) {
                    QuarryBlockEntity.this.purgeFilteredBufferItems();
                }
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

    public IItemHandler getCombinedItemHandler() {
        return bufferHandler;
    }

    public IItemHandler getBufferItemHandler() {
        return bufferHandler;
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

    public int getTotalAreaChunkCount() {
        return miningEngine.getTotalAreaChunkCount();
    }

    public int getProcessedAreaChunkCount() {
        return miningEngine.getProcessedAreaChunkCount();
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
        if (requiresChunkDiggingMode()) {
            return QuarryDiggingMode.CHUNK;
        }
        return diggingMode;
    }

    public boolean requiresChunkDiggingMode() {
        return QuarryAreaLogic.requiresChunkDiggingMode(sizeLeft, sizeRight, sizeDepth);
    }

    public void toggleDiggingMode() {
        if (requiresChunkDiggingMode()) {
            return;
        }
        diggingMode = diggingMode.toggle();
        miningEngine.invalidateQueue();
        setChanged();
    }

    public int getRedstoneMode() {
        return normalizeRedstoneMode(redstoneMode);
    }

    public void setRedstoneMode(int mode) {
        int newMode = normalizeRedstoneMode(mode);
        if (redstoneMode != newMode) {
            redstoneMode = newMode;
            setChanged();
        }
    }

    public void cycleRedstoneMode() {
        int next = (getRedstoneMode() + 1) % 5;
        if (next == 3) {
            next = 4;
        }
        setRedstoneMode(next);
    }

    public void cycleRedstoneModeBackward() {
        int prev = switch (getRedstoneMode()) {
            case 0 -> 4;
            case 1 -> 0;
            case 2 -> 1;
            case 4 -> 2;
            default -> 0;
        };
        setRedstoneMode(prev);
    }

    private static int normalizeRedstoneMode(int mode) {
        if (mode == 3) {
            return 4;
        }
        return Math.max(0, Math.min(mode, 4));
    }

    public int getMaxBlockCount() {
        return ModConfig.quarryMaxRange();
    }

    public int getMaxHeight() {
        return Math.max(MIN_HEIGHT_OR_DEPTH, ModConfig.quarryMaxHeight() - 1);
    }

    public int getMaxDepth() {
        return Math.max(MIN_HEIGHT_OR_DEPTH, ModConfig.quarryMaxRange() - 1);
    }

    public int getMaxWidth() {
        return Math.max(MIN_HEIGHT_OR_DEPTH, ModConfig.quarryMaxRange() - 1);
    }

    private void clampSizes() {
        sizeHeight = Math.max(MIN_HEIGHT_OR_DEPTH, Math.min(getMaxHeight(), sizeHeight));
        sizeDepth = Math.max(MIN_HEIGHT_OR_DEPTH, Math.min(getMaxDepth(), sizeDepth));
        sizeLeft = Math.max(0, sizeLeft);
        sizeRight = Math.max(0, sizeRight);
        int widthSum = sizeLeft + sizeRight;
        int maxWidth = getMaxWidth();
        if (widthSum > maxWidth) {
            int excess = widthSum - maxWidth;
            if (sizeLeft >= sizeRight) {
                sizeLeft = Math.max(0, sizeLeft - excess);
            } else {
                sizeRight = Math.max(0, sizeRight - excess);
            }
        }
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

    public void openMenu(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        serverPlayer.openMenu(this, buf -> buf.writeBlockPos(getBlockPos()));
    }

    public boolean isFilterModuleActive() {
        return QuarryEquipmentSlots.hasFilterModule(equipmentHandler);
    }

    public List<String> getActiveItemDenyFilters() {
        if (!isFilterModuleActive()) {
            return List.of();
        }
        ItemStack filterStack = equipmentHandler.getStackInSlot(QuarryEquipmentSlots.filterModuleSlot());
        return QuarryFilterModuleData.getDestroyList(filterStack);
    }

    /** Removes buffer stacks that match the active filter module destroy list. */
    public void purgeFilteredBufferItems() {
        if (!isFilterModuleActive() || level == null || level.isClientSide()) {
            return;
        }
        List<String> filters = getActiveItemDenyFilters();
        if (filters.isEmpty()) {
            return;
        }
        QuarryOutputHandler.purgeFilteredItemsFromBuffer(bufferHandler, filters, level.registryAccess());
    }

    public boolean canWork() {
        if (level == null || level.isClientSide()) {
            return getRedstoneMode() == 0;
        }
        return switch (getRedstoneMode()) {
            case 0 -> true;
            case 1 -> !QuarryRedstoneUtil.hasRedstoneSignal(level, worldPosition);
            case 2 -> QuarryRedstoneUtil.hasRedstoneSignal(level, worldPosition);
            case 4 -> false;
            default -> false;
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, QuarryBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        be.purgeFilteredBufferItems();
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("SizeLeft", sizeLeft);
        tag.putInt("SizeRight", sizeRight);
        tag.putInt("SizeHeight", sizeHeight);
        tag.putInt("SizeDepth", sizeDepth);
        tag.putBoolean("PreviewEnabled", previewEnabled);
        tag.putInt("DiggingMode", diggingMode.getId());
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putBoolean("PreviousCanWork", previousCanWork);
        tag.putInt("EquipmentVersion", QuarryEquipmentSlots.EQUIPMENT_LAYOUT_VERSION);
        tag.put("Buffer", bufferHandler.serializeNBT(registries));
        tag.put("Equipment", equipmentHandler.serializeNBT(registries));
        CompoundTag miningTag = new CompoundTag();
        miningEngine.save(miningTag, registries);
        tag.put("MiningEngine", miningTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sizeLeft = tag.contains("SizeLeft") ? tag.getInt("SizeLeft") : DEFAULT_SIZE_LEFT;
        sizeRight = tag.contains("SizeRight") ? tag.getInt("SizeRight") : DEFAULT_SIZE_RIGHT;
        sizeHeight = tag.contains("SizeHeight") ? tag.getInt("SizeHeight") : DEFAULT_SIZE_HEIGHT;
        sizeDepth = tag.contains("SizeDepth") ? tag.getInt("SizeDepth") : DEFAULT_SIZE_DEPTH;
        clampSizes();
        previewEnabled = tag.getBoolean("PreviewEnabled");
        diggingMode = QuarryDiggingMode.fromId(tag.contains("DiggingMode") ? tag.getInt("DiggingMode") : QuarryDiggingMode.VOLUME.getId());
        redstoneMode = normalizeRedstoneMode(tag.contains("RedstoneMode") ? tag.getInt("RedstoneMode") : 0);
        previousCanWork = tag.getBoolean("PreviousCanWork");
        energyStorage.setEnergy(tag.contains("Energy") ? tag.getInt("Energy") : 0);
        int equipmentVersion = tag.contains("EquipmentVersion") ? tag.getInt("EquipmentVersion") : 1;
        if (equipmentVersion < QuarryEquipmentSlots.EQUIPMENT_LAYOUT_VERSION) {
            migrateLegacyEquipment(tag, equipmentHandler, equipmentVersion, registries);
        } else {
            loadHandlerSlots(tag, "Equipment", equipmentHandler, registries);
        }
        loadHandlerSlots(tag, "Buffer", bufferHandler, registries);
        migrateRemovedDrills(equipmentHandler);
        if (tag.contains("MiningEngine", Tag.TAG_COMPOUND)) {
            miningEngine.load(tag.getCompound("MiningEngine"), registries);
        }
        refreshEnergyCapacity();
        if (level != null && !level.isClientSide()) {
            previousCanWork = canWork();
        }
    }

    private static void loadHandlerSlots(CompoundTag tag, String key, ItemStackHandler handler, HolderLookup.Provider registries) {
        if (tag.contains(key, Tag.TAG_COMPOUND)) {
            handler.deserializeNBT(registries, tag.getCompound(key));
            return;
        }
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.contains("Slot") ? entry.getInt("Slot") : entry.getInt("slot");
            ItemStack stack = ItemStack.parseOptional(registries, entry);
            if (slot >= 0 && slot < handler.getSlots()) {
                handler.setStackInSlot(slot, stack);
            }
        }
    }

    private static void migrateRemovedDrills(ItemStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            ItemStack migrated = ModItems.migrateRemovedDrill(stack);
            if (!ItemStack.matches(stack, migrated)) {
                handler.setStackInSlot(i, migrated);
            }
        }
    }

    private static final int LEGACY_V2_DIGGER_SLOT = 5;
    private static final int LEGACY_V2_SPEED_SLOT = 6;
    private static final int LEGACY_V2_ENCHANT_SLOT = 7;

    private static void migrateLegacyEquipment(CompoundTag tag, ItemStackHandler handler, int fromVersion, HolderLookup.Provider registries) {
        java.util.HashMap<Integer, ItemStack> bySlot = new java.util.HashMap<>();
        if (tag.contains("Equipment", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Equipment", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int slot = entry.contains("Slot") ? entry.getInt("Slot") : entry.getInt("slot");
                bySlot.put(slot, ItemStack.parseOptional(registries, entry));
            }
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
            } else if (ModItems.isDiamondDrill(stack)) {
                diamond = stack;
            }
        }

        if (!netherite.isEmpty()) {
            return singleDrillStack(netherite);
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
        stack = ModItems.migrateRemovedDrill(stack);
        if (ModItems.isAnyDrill(stack)) {
            return stack.copy();
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
}
