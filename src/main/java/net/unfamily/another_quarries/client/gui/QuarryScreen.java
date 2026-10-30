package net.unfamily.another_quarries.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.network.packet.QuarryDiggingModeC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryPreviewToggleC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryRebootC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryRedstoneModeC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarrySizeC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryTankDumpC2SPacket;
import net.unfamily.another_quarries.item.QuarryEquipmentSlots;
import net.unfamily.another_quarries.item.QuarryFilterModuleData;
import net.unfamily.another_quarries.item.QuarryModules;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.registry.ModItems;
import net.unfamily.another_quarries.util.QuarryDiggingMode;
import net.unfamily.another_quarries.client.QuarryFramePreviewRenderer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class QuarryScreen extends AbstractContainerScreen<QuarryMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            AnotherQuarries.MOD_ID, "textures/gui/backgrounds/quarry.png");
    private static final Identifier ENERGY_BAR_TEXTURE =
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/energy_bar.png");

    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = QuarryMenu.GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int TITLE_Y = 8;

    private static final int BUTTON_W = 14;
    private static final int BUTTON_H = 12;
    private static final int GAP = 3;
    private static final int ROW2_Y = QuarryMenu.CONTROL_BAND_TOP + 19;
    private static final int ROW1_Y = ROW2_Y - BUTTON_H - 2;
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;
    private static final int ENERGY_BAR_X = QuarryMenu.CONTROL_BAND_RIGHT - ENERGY_BAR_WIDTH - 5;
    private static final int ENERGY_BAR_Y = QuarryMenu.CONTROL_BAND_TOP + 1;
    private static final int REBOOT_BUTTON_W = 42;
    private static final int REBOOT_BUTTON_H = 12;
    private static final int REBOOT_BUTTON_X = ENERGY_BAR_X - GAP - REBOOT_BUTTON_W;
    private static final int REBOOT_BUTTON_Y = ROW2_Y;
    private static final int REDSTONE_X = REBOOT_BUTTON_X + (REBOOT_BUTTON_W - MachineGuiButtons.ICON_SIZE) / 2;
    private static final int REDSTONE_Y = REBOOT_BUTTON_Y - GAP - MachineGuiButtons.ICON_SIZE;
    /**
     * Left column: preview + digging mode stacked; arrow cluster to the right (Collecting Crate pattern).
     */
    private static final int PREVIEW_BUTTON_X = QuarryMenu.CONTROL_BAND_LEFT + 1;
    private static final int PREVIEW_BUTTON_W = 46;
    private static final int ARROW_GROUP_LEFT_X = PREVIEW_BUTTON_X + PREVIEW_BUTTON_W + GAP;
    private static final int ARROW_GROUP_WIDTH = 3 * BUTTON_W + 2 * GAP;
    private static final int ARROW_GROUP_CENTER_X = ARROW_GROUP_LEFT_X + ARROW_GROUP_WIDTH / 2;
    private static final int PREVIEW_BUTTON_Y = ROW1_Y;
    private static final int SIZE_LABEL_Y = ROW2_Y + BUTTON_H + 2;
    private static final int CHUNKS_LABEL_Y = QuarryMenu.EQUIPMENT_SLOTS_Y + 18 + 2;

    private static final int DIGGING_MODE_X = PREVIEW_BUTTON_X;
    private static final int DIGGING_MODE_Y = ROW2_Y;
    private static final int DIGGING_MODE_W = PREVIEW_BUTTON_W;

    private static final int DISABLED_SLOT_OVERLAY = 0xA0000000;

    /**
     * Tank frames baked into {@code quarry.png}: the fill area is 12x54 and the frame adds a 1px inset border.
     * Index order: 0 left-upper, 1 right-upper, 2 left-lower, 3 right-lower. Coordinates are the frame origin.
     */
    private static final int TANK_FILL_WIDTH = 12;
    private static final int TANK_FILL_HEIGHT = 54;
    private static final int TANK_FILL_INSET = 1;
    private static final int TANK_OUTER_W = TANK_FILL_WIDTH + 2 * TANK_FILL_INSET;
    private static final int TANK_OUTER_H = TANK_FILL_HEIGHT + 2 * TANK_FILL_INSET;
    private static final int[] TANK_BAR_X = {9, 197, 9, 197};
    private static final int[] TANK_BAR_Y = {23, 23, 97, 97};
    private static final int TANK_DUMP_W = 14;
    private static final int TANK_DUMP_H = 12;
    private static final int TANK_DUMP_GAP_BELOW = 3;

    private static final ItemStack GHOST_DRONE = new ItemStack(ModItems.DRONE.get());
    private static final ItemStack GHOST_DRILL = new ItemStack(ModItems.DRILL_DIAMOND.get());
    private static final ItemStack GHOST_DIGGER = new ItemStack(ModItems.MODULE_DIGGER.get());
    private static final ItemStack GHOST_SPEED = new ItemStack(ModItems.MODULE_SPEED.get());
    private static final List<ItemStack> ENCHANT_GHOST_STACKS = QuarryModules.enchantGhostStacks();

    private final GuiCycleTimer enchantGhostCycle = new GuiCycleTimer(() -> 1000);

    private Button closeButton;
    private Button rebootButton;
    private Button buttonUp;
    private Button buttonOffsetLeft;
    private Button buttonOffsetRight;
    private Button buttonLeft;
    private Button buttonRight;
    private Button buttonDepth;
    private Button buttonPreview;
    private Button diggingModeButton;
    private ItemIconButton redstoneButton;
    private final Button[] tankDumpButtons = new Button[QuarryBlockEntity.TANK_COUNT];
    private boolean previewButtonShowsHide;

    public QuarryScreen(QuarryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, QuarryMenu.GUI_WIDTH, QuarryMenu.GUI_HEIGHT);
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        closeButton = Button.builder(Component.literal("✕"), b -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
            }
        }).bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).build();
        addRenderableWidget(closeButton);

        rebootButton = Button.builder(Component.translatable("gui.another_quarries.quarry.reboot"), b -> sendReboot())
                .bounds(this.leftPos + REBOOT_BUTTON_X, this.topPos + REBOOT_BUTTON_Y, REBOOT_BUTTON_W, REBOOT_BUTTON_H)
                .build();
        rebootButton.setTooltip(Tooltip.create(Component.translatable("gui.another_quarries.quarry.reboot.tooltip")));
        addRenderableWidget(rebootButton);

        int upX = leftPos + ARROW_GROUP_LEFT_X + ARROW_GROUP_WIDTH / 2 - BUTTON_W / 2;
        int offsetLeftX = upX - BUTTON_W - GAP;
        int offsetRightX = upX + BUTTON_W + GAP;
        buttonUp = Button.builder(Component.literal("\u2191"), b -> sendSize(0, true))
                .bounds(upX, topPos + ROW1_Y, BUTTON_W, BUTTON_H).build();
        buttonOffsetLeft = Button.builder(Component.literal("\u00AB"), b -> sendSize(4, true))
                .bounds(offsetLeftX, topPos + ROW1_Y, BUTTON_W, BUTTON_H).build();
        buttonOffsetRight = Button.builder(Component.literal("\u00BB"), b -> sendSize(5, true))
                .bounds(offsetRightX, topPos + ROW1_Y, BUTTON_W, BUTTON_H).build();
        buttonLeft = Button.builder(Component.literal("\u2190"), b -> sendSize(1, true))
                .bounds(leftPos + ARROW_GROUP_LEFT_X, topPos + ROW2_Y, BUTTON_W, BUTTON_H).build();
        buttonDepth = Button.builder(Component.literal("-"), b -> sendSize(3, true))
                .bounds(leftPos + ARROW_GROUP_LEFT_X + BUTTON_W + GAP, topPos + ROW2_Y, BUTTON_W, BUTTON_H).build();
        buttonRight = Button.builder(Component.literal("\u2192"), b -> sendSize(2, true))
                .bounds(leftPos + ARROW_GROUP_LEFT_X + 2 * (BUTTON_W + GAP), topPos + ROW2_Y, BUTTON_W, BUTTON_H).build();
        addRenderableWidget(buttonUp);
        addRenderableWidget(buttonOffsetLeft);
        addRenderableWidget(buttonOffsetRight);
        addRenderableWidget(buttonLeft);
        addRenderableWidget(buttonDepth);
        addRenderableWidget(buttonRight);

        buttonPreview = Button.builder(Component.translatable("gui.another_quarries.quarry.preview"), b -> togglePreview())
                .bounds(leftPos + PREVIEW_BUTTON_X, topPos + PREVIEW_BUTTON_Y, PREVIEW_BUTTON_W, BUTTON_H).build();
        buttonPreview.setTooltip(Tooltip.create(Component.translatable("gui.another_quarries.quarry.preview.tooltip")));
        addRenderableWidget(buttonPreview);

        diggingModeButton = Button.builder(
                        Component.translatable("gui.another_quarries.quarry.digging_mode.volume"),
                        b -> sendDiggingModeToggle())
                .bounds(leftPos + DIGGING_MODE_X, topPos + DIGGING_MODE_Y, DIGGING_MODE_W, BUTTON_H)
                .build();
        addRenderableWidget(diggingModeButton);

        for (int tank = 0; tank < QuarryBlockEntity.TANK_COUNT; tank++) {
            final int tankIndex = tank;
            int dumpX = leftPos + TANK_BAR_X[tank] + (TANK_OUTER_W - TANK_DUMP_W) / 2;
            int dumpY = topPos + tankDumpY(tank);
            Button dump = Button.builder(Component.literal("D"), b -> sendTankDump(tankIndex))
                    .bounds(dumpX, dumpY, TANK_DUMP_W, TANK_DUMP_H).build();
            dump.setTooltip(Tooltip.create(Component.translatable("gui.another_quarries.quarry.tank_dump.tooltip")));
            tankDumpButtons[tank] = addRenderableWidget(dump);
        }

        redstoneButton = addRenderableWidget(MachineGuiButtons.redstoneIconButton(
                leftPos + REDSTONE_X, topPos + REDSTONE_Y, b -> sendRedstone(false), menu::getRedstoneMode, false));

        updateButtonTooltips();
        previewButtonShowsHide = menu.isPreviewEnabled();
        updatePreviewButtonLabel();
        updateDiggingModeButton();
        if (menu.isPreviewEnabled()) {
            BlockPos pos = menu.getSyncedBlockPos();
            if (!pos.equals(BlockPos.ZERO)) {
                ClientPacketDistributor.sendToServer(new QuarryPreviewToggleC2SPacket(pos, true));
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonTooltips();
        if (menu.isPreviewEnabled() != previewButtonShowsHide) {
            previewButtonShowsHide = menu.isPreviewEnabled();
            updatePreviewButtonLabel();
        }
        updateDiggingModeButton();
        updateTankDumpButtons();
    }

    private static int tankDumpY(int tank) {
        return TANK_BAR_Y[tank] + TANK_OUTER_H + TANK_DUMP_GAP_BELOW;
    }

    private void updateTankDumpButtons() {
        for (int tank = 0; tank < tankDumpButtons.length; tank++) {
            if (tankDumpButtons[tank] != null) {
                tankDumpButtons[tank].active = menu.getTankAmount(tank) > 0;
            }
        }
    }

    private void sendTankDump(int tank) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO) || menu.getTankAmount(tank) <= 0) {
            return;
        }
        ClientPacketDistributor.sendToServer(new QuarryTankDumpC2SPacket(pos, tank));
        playButtonSound();
    }

    private void updateDiggingModeButton() {
        if (diggingModeButton == null) {
            return;
        }
        boolean forcedChunk = menu.requiresChunkDiggingMode();
        diggingModeButton.active = !forcedChunk;

        boolean chunkMode = menu.getDiggingModeId() == QuarryDiggingMode.CHUNK.getId();
        if (chunkMode) {
            diggingModeButton.setMessage(Component.translatable("gui.another_quarries.quarry.digging_mode.chunk"));
            diggingModeButton.setTooltip(Tooltip.create(forcedChunk
                    ? Component.translatable("gui.another_quarries.quarry.digging_mode.chunk.disabled.tooltip",
                    ModConfig.volumeModeMaxFootprint())
                    : Component.translatable("gui.another_quarries.quarry.digging_mode.chunk.tooltip")));
        } else {
            diggingModeButton.setMessage(Component.translatable("gui.another_quarries.quarry.digging_mode.volume"));
            diggingModeButton.setTooltip(Tooltip.create(
                    Component.translatable("gui.another_quarries.quarry.digging_mode.volume.tooltip")));
        }
    }

    private void updatePreviewButtonLabel() {
        if (buttonPreview != null) {
            buttonPreview.setMessage(Component.translatable(
                    previewButtonShowsHide ? "gui.another_quarries.quarry.hide" : "gui.another_quarries.quarry.preview"));
        }
    }

    private static Tooltip tooltipWithValue(String valueKey, int value) {
        Component full = Component.translatable(valueKey, value)
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.another_quarries.quarry.tooltip.left_click"))
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.another_quarries.quarry.tooltip.right_click"))
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.another_quarries.quarry.tooltip.shift_10"))
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.another_quarries.quarry.tooltip.alt_ctrl_5"));
        return Tooltip.create(full);
    }

    private void updateButtonTooltips() {
        if (buttonUp != null) {
            buttonUp.setTooltip(tooltipWithValue("gui.another_quarries.quarry.tooltip.up_value", menu.getAreaHeight()));
        }
        if (buttonOffsetLeft != null) {
            buttonOffsetLeft.setTooltip(offsetTooltip("gui.another_quarries.quarry.tooltip.offset_left"));
        }
        if (buttonOffsetRight != null) {
            buttonOffsetRight.setTooltip(offsetTooltip("gui.another_quarries.quarry.tooltip.offset_right"));
        }
        if (buttonLeft != null) {
            buttonLeft.setTooltip(tooltipWithValue("gui.another_quarries.quarry.tooltip.left_value", menu.getSizeLeft()));
        }
        if (buttonRight != null) {
            buttonRight.setTooltip(tooltipWithValue("gui.another_quarries.quarry.tooltip.right_value", menu.getSizeRight()));
        }
        if (buttonDepth != null) {
            buttonDepth.setTooltip(tooltipWithValue("gui.another_quarries.quarry.tooltip.depth_value", menu.getAreaDepth()));
        }
    }

    private static Tooltip offsetTooltip(String key) {
        Component full = Component.translatable(key)
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.another_quarries.quarry.tooltip.left_click"))
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.another_quarries.quarry.tooltip.right_click"))
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.another_quarries.quarry.tooltip.shift_10"))
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.another_quarries.quarry.tooltip.alt_ctrl_5"));
        return Tooltip.create(full);
    }

    private int modifierStepAmount() {
        if (minecraft == null || minecraft.getWindow() == null) {
            return 1;
        }
        var window = minecraft.getWindow();
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            return 10;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT)) {
            return 5;
        }
        return 1;
    }

    private void sendSize(int direction, boolean increment) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        ClientPacketDistributor.sendToServer(new QuarrySizeC2SPacket(pos, direction, increment, modifierStepAmount()));
        playButtonSound();
    }

    private void togglePreview() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        playButtonSound();
        boolean enabling = !menu.isPreviewEnabled();
        QuarryFramePreviewRenderer.getInstance().clearArea(pos);
        ClientPacketDistributor.sendToServer(new QuarryPreviewToggleC2SPacket(pos, enabling));
        previewButtonShowsHide = enabling;
        updatePreviewButtonLabel();
    }

    private void sendReboot() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        ClientPacketDistributor.sendToServer(new QuarryRebootC2SPacket(pos));
        playButtonSound();
    }

    private void sendDiggingModeToggle() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        ClientPacketDistributor.sendToServer(new QuarryDiggingModeC2SPacket(pos));
        playButtonSound();
    }

    private void sendRedstone(boolean backward) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ClientPacketDistributor.sendToServer(new QuarryRedstoneModeC2SPacket(pos, backward));
            playButtonSound();
        }
    }

    private void playButtonSound() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        }
    }

    private void drawCenteredText(GuiGraphicsExtractor guiGraphics, Component text, int centerX, int y, int color) {
        guiGraphics.text(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, QuarryMenu.GUI_WIDTH, QuarryMenu.GUI_HEIGHT);
        renderTankFluids(guiGraphics);
        renderGhostItems(guiGraphics);
        renderDisabledEquipmentSlots(guiGraphics);
    }

    private void renderTankFluids(GuiGraphicsExtractor guiGraphics) {
        for (int tank = 0; tank < QuarryBlockEntity.TANK_COUNT; tank++) {
            int amount = menu.getTankAmount(tank);
            int capacity = menu.getTankCapacity(tank);
            int fluidId = menu.getTankFluidId(tank);
            if (capacity <= 0 || amount <= 0 || fluidId < 0) {
                continue;
            }
            Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
            if (fluid == null || fluid == Fluids.EMPTY) {
                continue;
            }
            int fillPixels = Math.min(TANK_FILL_HEIGHT, (TANK_FILL_HEIGHT * amount) / capacity);
            if (fillPixels <= 0 && amount > 0) {
                fillPixels = 1;
            }
            int fillLeft = leftPos + TANK_BAR_X[tank] + TANK_FILL_INSET;
            int fillBottom = topPos + TANK_BAR_Y[tank] + TANK_FILL_INSET + TANK_FILL_HEIGHT;
            FluidRenderHelper.drawFluidInTank(
                    guiGraphics, new FluidStack(fluid, amount), fillLeft, fillBottom - fillPixels,
                    TANK_FILL_WIDTH, fillPixels);
        }
    }

    private void renderDisabledEquipmentSlots(GuiGraphicsExtractor guiGraphics) {
        for (int equipmentSlot = 0; equipmentSlot < QuarryEquipmentSlots.slotCount(); equipmentSlot++) {
            Slot slot = menu.getSlot(QuarryMenu.equipmentMenuIndex(equipmentSlot));
            if (slot != null && !slot.isActive()) {
                renderDisabledSlotOverlay(guiGraphics, slot.x, slot.y);
            }
        }
        for (int col = QuarryEquipmentSlots.slotCount(); col < QuarryEquipmentSlots.guiColumnCount(); col++) {
            renderDisabledSlotOverlay(guiGraphics, QuarryMenu.EQUIPMENT_SLOTS_X + col * 18, QuarryMenu.EQUIPMENT_SLOTS_Y);
        }
    }

    private void renderDisabledSlotOverlay(GuiGraphicsExtractor guiGraphics, int slotX, int slotY) {
        int x = this.leftPos + slotX;
        int y = this.topPos + slotY;
        guiGraphics.fill(x, y, x + 16, y + 16, DISABLED_SLOT_OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        renderEnergyBar(guiGraphics);
        renderAreaLabel(guiGraphics);
        renderChunksProgressLabel(guiGraphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int titleWidth = this.font.width(this.title);
        int titleX = (this.imageWidth - titleWidth) / 2;
        guiGraphics.text(this.font, this.title, titleX, TITLE_Y, GuiTextColors.TITLE, false);
    }

    private void renderAreaLabel(GuiGraphicsExtractor guiGraphics) {
        int labelCenterX = this.leftPos + ARROW_GROUP_CENTER_X;
        Component sizeLabel = Component.translatable(
                "gui.another_quarries.quarry.size",
                menu.getSizeLeft(),
                menu.getSizeRight(),
                menu.getAreaWidth(),
                menu.getAreaHeight(),
                menu.getAreaDepth());
        drawCenteredText(guiGraphics, sizeLabel, labelCenterX, this.topPos + SIZE_LABEL_Y, GuiTextColors.TITLE);
    }

    private void renderChunksProgressLabel(GuiGraphicsExtractor guiGraphics) {
        int centerX = this.leftPos + this.imageWidth / 2;
        if (isPumpDrainListEmpty()) {
            Component label = Component.translatable("gui.another_quarries.quarry.pump_no_fluids");
            drawCenteredText(guiGraphics, label, centerX, this.topPos + CHUNKS_LABEL_Y, GuiTextColors.ERROR);
            return;
        }
        int total = menu.getTotalAreaChunkCount();
        if (total <= 1) {
            return;
        }
        Component label = Component.translatable(
                "gui.another_quarries.quarry.chunks_progress",
                menu.getProcessedAreaChunkCount(),
                total);
        drawCenteredText(guiGraphics, label, centerX, this.topPos + CHUNKS_LABEL_Y, GuiTextColors.TITLE);
    }

    private boolean isPumpDrainListEmpty() {
        Slot slot = menu.getSlot(QuarryMenu.pumpModuleMenuIndex());
        ItemStack stack = slot.getItem();
        return QuarryFilterModuleData.isPumpModule(stack) && !QuarryFilterModuleData.hasConfiguredLines(stack);
    }

    private void renderGhostItems(GuiGraphicsExtractor guiGraphics) {
        for (int equipmentSlot = 0; equipmentSlot < QuarryEquipmentSlots.slotCount(); equipmentSlot++) {
            Slot slot = menu.getSlot(QuarryMenu.equipmentMenuIndex(equipmentSlot));
            if (equipmentSlot == QuarryEquipmentSlots.enchantModuleSlot()) {
                GuiGhostItem.renderCycling(
                        guiGraphics,
                        leftPos,
                        topPos,
                        slot,
                        ENCHANT_GHOST_STACKS,
                        enchantGhostCycle,
                        GuiGhostItem.DEFAULT_ARGB);
                continue;
            }
            ItemStack ghost = ghostForEquipmentSlot(equipmentSlot);
            if (!ghost.isEmpty()) {
                GuiGhostItem.render(guiGraphics, leftPos, topPos, slot, ghost, GuiGhostItem.DEFAULT_ARGB);
            }
        }
    }

    private void renderEnergyBar(GuiGraphicsExtractor guiGraphics) {
        int energy = menu.getEnergyStored();
        int maxEnergy = Math.max(1, menu.getMaxEnergyStored());
        int barX = leftPos + ENERGY_BAR_X;
        int barY = topPos + ENERGY_BAR_Y;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR_TEXTURE, barX, barY,
                8.0F, 0.0F, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, 16, 32);
        if (energy > 0) {
            int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
            int energyY = barY + (ENERGY_BAR_HEIGHT - energyHeight);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR_TEXTURE, barX, energyY,
                    0.0F, (float) (ENERGY_BAR_HEIGHT - energyHeight), ENERGY_BAR_WIDTH, energyHeight, 16, 32);
        }
    }

    @Override
    public void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractTooltip(guiGraphics, mouseX, mouseY);
        int barX = leftPos + ENERGY_BAR_X;
        int barY = topPos + ENERGY_BAR_Y;
        if (mouseX >= barX && mouseX <= barX + ENERGY_BAR_WIDTH && mouseY >= barY && mouseY <= barY + ENERGY_BAR_HEIGHT) {
            int energy = menu.getEnergyStored();
            int maxEnergy = menu.getMaxEnergyStored();
            int rfPerBlock = menu.getEstimatedRfPerBlock();
            int baseRf = ModConfig.baseRfPerBlock();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.another_quarries.quarry.energy",
                    String.format("%,d", energy), String.format("%,d", maxEnergy)));
            lines.add(Component.translatable("gui.another_quarries.quarry.rf_per_block_worker",
                    String.format("%,d", rfPerBlock)));
            if (rfPerBlock != baseRf) {
                lines.add(Component.translatable("gui.another_quarries.quarry.rf_per_block_breakdown",
                        String.format("%,d", baseRf), String.format("%,d", rfPerBlock - baseRf)));
            }
            guiGraphics.setTooltipForNextFrame(
                    this.font,
                    lines.stream().map(Component::getVisualOrderText).toList(),
                    DefaultTooltipPositioner.INSTANCE,
                    mouseX,
                    mouseY,
                    true);
        }
        renderTankTooltips(guiGraphics, mouseX, mouseY);
        renderGhostTooltips(guiGraphics, mouseX, mouseY);
        if (redstoneButton != null && redstoneButton.isMouseOver(mouseX, mouseY)) {
            MachineGuiButtons.renderTooltipLine(guiGraphics, font, mouseX, mouseY,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), false));
        }
    }

    private void renderTankTooltips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        for (int tank = 0; tank < QuarryBlockEntity.TANK_COUNT; tank++) {
            int left = leftPos + TANK_BAR_X[tank] + TANK_FILL_INSET;
            int top = topPos + TANK_BAR_Y[tank] + TANK_FILL_INSET;
            if (mouseX < left || mouseX >= left + TANK_FILL_WIDTH || mouseY < top || mouseY >= top + TANK_FILL_HEIGHT) {
                continue;
            }
            int amount = menu.getTankAmount(tank);
            int capacity = menu.getTankCapacity(tank);
            int fluidId = menu.getTankFluidId(tank);
            List<Component> lines = new ArrayList<>();
            lines.add(capacity > 0 && amount > 0
                    ? Component.translatable("gui.another_quarries.quarry.tank_tooltip", amount, capacity)
                    : Component.translatable("gui.another_quarries.quarry.tank_empty"));
            if (fluidId >= 0) {
                Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    lines.add(Component.translatable(fluid.getFluidType().getDescriptionId()));
                }
            }
            guiGraphics.setTooltipForNextFrame(
                    this.font,
                    lines.stream().map(Component::getVisualOrderText).toList(),
                    DefaultTooltipPositioner.INSTANCE,
                    mouseX,
                    mouseY,
                    true);
            return;
        }
    }

    private void renderGhostTooltips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (!this.menu.getCarried().isEmpty()) {
            return;
        }
        for (int equipmentSlot = 0; equipmentSlot < QuarryEquipmentSlots.slotCount(); equipmentSlot++) {
            Slot slot = menu.getSlot(QuarryMenu.equipmentMenuIndex(equipmentSlot));
            if (slot == null || !slot.getItem().isEmpty() || !isMouseOverSlot(slot, mouseX, mouseY)) {
                continue;
            }
            if (equipmentSlot == QuarryEquipmentSlots.enchantModuleSlot()) {
                ItemStack ghost = enchantGhostCycle.getOrDefault(ENCHANT_GHOST_STACKS, ENCHANT_GHOST_STACKS.get(0));
                guiGraphics.setTooltipForNextFrame(this.font, ghost, mouseX, mouseY);
                return;
            }
            ItemStack ghost = ghostForEquipmentSlot(equipmentSlot);
            if (!ghost.isEmpty()) {
                guiGraphics.setTooltipForNextFrame(this.font, ghost, mouseX, mouseY);
                return;
            }
        }
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private ItemStack ghostForEquipmentSlot(int equipmentSlot) {
        if (QuarryEquipmentSlots.isDroneSlot(equipmentSlot)) {
            return GHOST_DRONE;
        }
        if (QuarryEquipmentSlots.isDrillSlot(equipmentSlot)) {
            return GHOST_DRILL;
        }
        if (equipmentSlot == QuarryEquipmentSlots.diggerModuleSlot()) {
            return GHOST_DIGGER;
        }
        if (equipmentSlot == QuarryEquipmentSlots.speedModuleSlot()) {
            return GHOST_SPEED;
        }
        if (equipmentSlot == QuarryEquipmentSlots.filterModuleSlot()) {
            return new ItemStack(ModItems.MODULE_FILTER.get());
        }
        if (equipmentSlot == QuarryEquipmentSlots.pumpModuleSlot()) {
            return new ItemStack(ModItems.MODULE_PUMP.get());
        }
        return ItemStack.EMPTY;
    }

    private ItemStack ghostForSlot(int menuIndex) {
        int equipmentSlot = menuIndex - QuarryMenu.BUFFER_SLOT_COUNT;
        if (equipmentSlot >= 0 && equipmentSlot < QuarryEquipmentSlots.slotCount()) {
            return ghostForEquipmentSlot(equipmentSlot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1) {
            double mouseX = event.x();
            double mouseY = event.y();
            if (buttonUp != null && buttonUp.isMouseOver(mouseX, mouseY)) {
                sendSize(0, false);
                return true;
            }
            if (buttonOffsetLeft != null && buttonOffsetLeft.isMouseOver(mouseX, mouseY)) {
                sendSize(4, false);
                return true;
            }
            if (buttonOffsetRight != null && buttonOffsetRight.isMouseOver(mouseX, mouseY)) {
                sendSize(5, false);
                return true;
            }
            if (buttonLeft != null && buttonLeft.isMouseOver(mouseX, mouseY)) {
                sendSize(1, false);
                return true;
            }
            if (buttonRight != null && buttonRight.isMouseOver(mouseX, mouseY)) {
                sendSize(2, false);
                return true;
            }
            if (buttonDepth != null && buttonDepth.isMouseOver(mouseX, mouseY)) {
                sendSize(3, false);
                return true;
            }
            if (redstoneButton != null && redstoneButton.isMouseOver(mouseX, mouseY)) {
                sendRedstone(true);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}

