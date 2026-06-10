package net.unfamily.another_quarries.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.item.QuarryEquipmentSlots;
import net.unfamily.another_quarries.registry.ModBlocks;
import net.unfamily.another_quarries.registry.ModItems;
import net.unfamily.another_quarries.registry.ModMenuTypes;
import net.unfamily.another_quarries.util.QuarryAreaLogic;

public class QuarryMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 240;

    public static final int BUFFER_SLOTS_X = 8;
    public static final int BUFFER_SLOTS_Y = 25;
    public static final int EQUIPMENT_SLOTS_X = 8;
    public static final int EQUIPMENT_SLOTS_Y = 126;
    public static final int PLAYER_INVENTORY_X = 8;
    public static final int PLAYER_INVENTORY_Y = 158;
    public static final int PLAYER_HOTBAR_Y = 216;

    public static final int CONTROL_BAND_LEFT = 7;
    public static final int CONTROL_BAND_TOP = 81;
    public static final int CONTROL_BAND_RIGHT = 168;
    public static final int CONTROL_BAND_BOTTOM = 121;
    public static final int CONTROL_BAND_WIDTH = CONTROL_BAND_RIGHT - CONTROL_BAND_LEFT;
    public static final int CONTROL_BAND_HEIGHT = CONTROL_BAND_BOTTOM - CONTROL_BAND_TOP;

    public static final int BUFFER_SLOT_COUNT = QuarryBlockEntity.BUFFER_SLOT_COUNT;
    public static int equipmentSlotCount() {
        return QuarryEquipmentSlots.slotCount();
    }

    public static int equipmentMenuIndex(int equipmentSlot) {
        return BUFFER_SLOT_COUNT + equipmentSlot;
    }

    public static int firstDroneMenuIndex() {
        return equipmentMenuIndex(QuarryEquipmentSlots.firstDroneSlot());
    }

    public static int droneMenuIndexEnd() {
        return equipmentMenuIndex(QuarryEquipmentSlots.drillSlotStart());
    }

    public static int firstDrillMenuIndex() {
        return equipmentMenuIndex(QuarryEquipmentSlots.drillSlotStart());
    }

    public static int drillMenuIndexEnd() {
        return equipmentMenuIndex(QuarryEquipmentSlots.diggerModuleSlot());
    }

    public static int diggerModuleMenuIndex() {
        return equipmentMenuIndex(QuarryEquipmentSlots.diggerModuleSlot());
    }

    public static int speedModuleMenuIndex() {
        return equipmentMenuIndex(QuarryEquipmentSlots.speedModuleSlot());
    }

    public static int enchantModuleMenuIndex() {
        return equipmentMenuIndex(QuarryEquipmentSlots.enchantModuleSlot());
    }

    /** @deprecated use {@link #firstDroneMenuIndex()} */
    @Deprecated
    public static final int DRONE_SLOT_INDEX = BUFFER_SLOT_COUNT + QuarryEquipmentSlots.firstDroneSlot();
    /** @deprecated use {@link #firstDrillMenuIndex()} */
    @Deprecated
    public static final int DRILL_SLOT_INDEX = BUFFER_SLOT_COUNT + 1;
    /** @deprecated use {@link #diggerModuleMenuIndex()} */
    @Deprecated
    public static final int DIGGER_MODULE_SLOT_INDEX = BUFFER_SLOT_COUNT + 2;
    /** @deprecated use {@link #speedModuleMenuIndex()} */
    @Deprecated
    public static final int SPEED_MODULE_SLOT_INDEX = BUFFER_SLOT_COUNT + 3;
    /** @deprecated use {@link #enchantModuleMenuIndex()} */
    @Deprecated
    public static final int ENCHANT_MODULE_SLOT_INDEX = BUFFER_SLOT_COUNT + 4;
    /** @deprecated unused reserved GUI column */
    @Deprecated
    public static final int FILTER_SLOT_INDEX = BUFFER_SLOT_COUNT + 5;
    /** @deprecated use {@link #diggerModuleMenuIndex()} */
    @Deprecated
    public static final int MODULE_SLOT_START = DIGGER_MODULE_SLOT_INDEX;
    /** @deprecated */
    @Deprecated
    public static final int MODULE_SLOT_COUNT = 3;
    public static final int PLAYER_SLOT_START = BUFFER_SLOT_COUNT + QuarryEquipmentSlots.guiColumnCount();

    private static final int ENERGY_INDEX = 0;
    private static final int MAX_ENERGY_INDEX = 1;
    private static final int REDSTONE_MODE_INDEX = 2;
    private static final int DIGGING_MODE_INDEX = 3;
    private static final int PREVIEW_ENABLED_INDEX = 4;
    private static final int SIZE_LEFT_INDEX = 5;
    private static final int SIZE_RIGHT_INDEX = 6;
    private static final int SIZE_HEIGHT_INDEX = 7;
    private static final int SIZE_DEPTH_INDEX = 8;
    private static final int RF_PER_BLOCK_INDEX = 9;
    private static final int FACING_INDEX = 10;
    private static final int BLOCK_POS_X_INDEX = 11;
    private static final int BLOCK_POS_Y_INDEX = 12;
    private static final int BLOCK_POS_Z_INDEX = 13;
    private static final int PROCESSED_CHUNKS_INDEX = 14;
    private static final int TOTAL_CHUNKS_INDEX = 15;
    private static final int DATA_COUNT = 16;

    private final QuarryBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    private final ContainerData containerData;

    public QuarryMenu(int containerId, Inventory playerInventory, QuarryBlockEntity blockEntity) {
        super(ModMenuTypes.QUARRY_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.containerData = createContainerData(blockEntity);
        this.addDataSlots(this.containerData);
        addBufferSlots(blockEntity.getBufferHandler());
        addEquipmentSlots(blockEntity.getEquipmentHandler());
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public QuarryMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.QUARRY_MENU.get(), containerId);
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.containerData = new SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);
        ItemStackHandler buffer = new ItemStackHandler(BUFFER_SLOT_COUNT);
        ItemStackHandler equipment = new ItemStackHandler(QuarryMenu.equipmentSlotCount());
        addBufferSlots(buffer);
        addEquipmentSlots(equipment);
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private static ContainerData createContainerData(QuarryBlockEntity be) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case ENERGY_INDEX -> be.getEnergyStorage().getEnergyStored();
                    case MAX_ENERGY_INDEX -> be.getEnergyStorage().getMaxEnergyStored();
                    case REDSTONE_MODE_INDEX -> be.getRedstoneMode();
                    case DIGGING_MODE_INDEX -> be.getDiggingMode().getId();
                    case PREVIEW_ENABLED_INDEX -> be.isPreviewEnabled() ? 1 : 0;
                    case SIZE_LEFT_INDEX -> be.getSizeLeft();
                    case SIZE_RIGHT_INDEX -> be.getSizeRight();
                    case SIZE_HEIGHT_INDEX -> be.getSizeHeight();
                    case SIZE_DEPTH_INDEX -> be.getSizeDepth();
                    case RF_PER_BLOCK_INDEX -> be.estimatedRfPerBlock();
                    case FACING_INDEX -> {
                        if (be.getLevel() != null) {
                            yield be.getLevel()
                                    .getBlockState(be.getBlockPos())
                                    .getValue(HorizontalDirectionalBlock.FACING)
                                    .get2DDataValue();
                        }
                        yield 0;
                    }
                    case BLOCK_POS_X_INDEX -> be.getBlockPos().getX();
                    case BLOCK_POS_Y_INDEX -> be.getBlockPos().getY();
                    case BLOCK_POS_Z_INDEX -> be.getBlockPos().getZ();
                    case PROCESSED_CHUNKS_INDEX -> be.getProcessedAreaChunkCount();
                    case TOTAL_CHUNKS_INDEX -> be.getTotalAreaChunkCount();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private void addBufferSlots(IItemHandler handler) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9;
                addSlot(new SlotItemHandler(handler, slotIndex,
                        BUFFER_SLOTS_X + col * 18,
                        BUFFER_SLOTS_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !ModItems.isQuarryEquipment(stack);
                    }
                });
            }
        }
    }

    private void addEquipmentSlots(IItemHandler handler) {
        for (int col = 0; col < QuarryEquipmentSlots.slotCount(); col++) {
            int slotIndex = col;
            addSlot(new SlotItemHandler(handler, slotIndex,
                    EQUIPMENT_SLOTS_X + col * 18,
                    EQUIPMENT_SLOTS_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return handler.isItemValid(slotIndex, stack);
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return handler.getSlotLimit(slotIndex);
                }
            });
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9;
                addSlot(new Slot(playerInventory, slotIndex,
                        PLAYER_INVENTORY_X + col * 18,
                        PLAYER_INVENTORY_Y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    PLAYER_INVENTORY_X + col * 18,
                    PLAYER_HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.QUARRY.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            if (index < PLAYER_SLOT_START) {
                if (index < BUFFER_SLOT_COUNT && ModItems.isQuarryEquipment(slotStack)) {
                    if (!moveEquipmentFromStack(slotStack)
                            && !this.moveItemStackTo(slotStack, PLAYER_SLOT_START, 64, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(slotStack, PLAYER_SLOT_START, 64, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (ModItems.isQuarryEquipment(slotStack)) {
                if (!moveEquipmentFromStack(slotStack)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, BUFFER_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    private boolean moveToEquipment(ItemStack stack, int start, int end) {
        return this.moveItemStackTo(stack, start, end, false);
    }

    private boolean moveEquipmentFromStack(ItemStack stack) {
        if (ModItems.isDrone(stack)) {
            return moveToEquipment(stack, firstDroneMenuIndex(), droneMenuIndexEnd());
        }
        if (ModItems.isAnyDrill(stack)) {
            return moveToEquipment(stack, firstDrillMenuIndex(), drillMenuIndexEnd());
        }
        if (stack.is(ModItems.MODULE_DIGGER.get())) {
            return moveToEquipment(stack, diggerModuleMenuIndex(), diggerModuleMenuIndex() + 1);
        }
        if (stack.is(ModItems.MODULE_SPEED.get())) {
            return moveToEquipment(stack, speedModuleMenuIndex(), speedModuleMenuIndex() + 1);
        }
        if (stack.is(ModItems.MODULE_FORTUNE.get()) || stack.is(ModItems.MODULE_SILK_TOUCH.get())) {
            return moveToEquipment(stack, enchantModuleMenuIndex(), enchantModuleMenuIndex() + 1);
        }
        return false;
    }

    public QuarryBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getEnergyStored() {
        return containerData.get(ENERGY_INDEX);
    }

    public int getMaxEnergyStored() {
        return containerData.get(MAX_ENERGY_INDEX);
    }

    public int getRedstoneMode() {
        return containerData.get(REDSTONE_MODE_INDEX);
    }

    public int getDiggingModeId() {
        return containerData.get(DIGGING_MODE_INDEX);
    }

    public boolean requiresChunkDiggingMode() {
        if (blockEntity != null) {
            return blockEntity.requiresChunkDiggingMode();
        }
        return QuarryAreaLogic.requiresChunkDiggingMode(
                containerData.get(SIZE_LEFT_INDEX),
                containerData.get(SIZE_RIGHT_INDEX),
                containerData.get(SIZE_DEPTH_INDEX));
    }

    public int getEstimatedRfPerBlock() {
        return containerData.get(RF_PER_BLOCK_INDEX);
    }

    public int getSizeLeft() {
        return containerData.get(SIZE_RIGHT_INDEX);
    }

    public int getSizeRight() {
        return containerData.get(SIZE_LEFT_INDEX);
    }

    public int getSizeHeight() {
        return containerData.get(SIZE_HEIGHT_INDEX);
    }

    public int getSizeDepth() {
        return containerData.get(SIZE_DEPTH_INDEX);
    }

    public int getAreaWidth() {
        return QuarryAreaLogic.blockWidth(getSizeLeft(), getSizeRight());
    }

    public int getAreaHeight() {
        return QuarryAreaLogic.blockHeight(getSizeHeight());
    }

    public int getAreaDepth() {
        return QuarryAreaLogic.blockDepth(getSizeDepth());
    }

    public boolean isPreviewEnabled() {
        return containerData.get(PREVIEW_ENABLED_INDEX) != 0;
    }

    public Direction getFacing() {
        return Direction.from2DDataValue(containerData.get(FACING_INDEX));
    }

    public BlockPos getSyncedBlockPos() {
        if (this.blockEntity != null) {
            return this.blockPos;
        }
        int x = this.containerData.get(BLOCK_POS_X_INDEX);
        int y = this.containerData.get(BLOCK_POS_Y_INDEX);
        int z = this.containerData.get(BLOCK_POS_Z_INDEX);
        if (x == 0 && y == 0 && z == 0) {
            return this.blockPos;
        }
        return new BlockPos(x, y, z);
    }

    public int getProcessedAreaChunkCount() {
        return containerData.get(PROCESSED_CHUNKS_INDEX);
    }

    public int getTotalAreaChunkCount() {
        return containerData.get(TOTAL_CHUNKS_INDEX);
    }
}
