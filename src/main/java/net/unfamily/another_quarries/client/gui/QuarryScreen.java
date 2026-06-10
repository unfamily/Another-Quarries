package net.unfamily.another_quarries.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.item.QuarryEquipmentSlots;
import net.unfamily.another_quarries.item.QuarryModules;
import net.unfamily.another_quarries.network.packet.QuarryDiggingModeC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryPreviewToggleC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryRebootC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryRedstoneModeC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarrySizeC2SPacket;
import net.unfamily.another_quarries.registry.ModItems;
import net.unfamily.another_quarries.util.QuarryDiggingMode;
import net.unfamily.iskalib.client.marker.MarkRenderer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class QuarryScreen extends AbstractContainerScreen<QuarryMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            AnotherQuarries.MOD_ID, "textures/gui/backgrounds/quarry.png");
    private static final ResourceLocation ENERGY_BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/energy_bar.png");

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
    private static final int PREVIEW_BUTTON_X = QuarryMenu.CONTROL_BAND_LEFT + 1;
    private static final int PREVIEW_BUTTON_W = 46;
    private static final int ARROW_GROUP_LEFT_X = PREVIEW_BUTTON_X + PREVIEW_BUTTON_W + GAP;
    private static final int ARROW_GROUP_WIDTH = 3 * BUTTON_W + 2 * GAP;
    private static final int ARROW_GROUP_CENTER_X = ARROW_GROUP_LEFT_X + ARROW_GROUP_WIDTH / 2;
    private static final int PREVIEW_BUTTON_Y = ROW1_Y;
    private static final int SIZE_LABEL_Y = ROW2_Y + BUTTON_H + 2;
    private static final int CHUNKS_LABEL_Y = QuarryMenu.EQUIPMENT_SLOTS_Y + 18;
    private static final int DIGGING_MODE_X = PREVIEW_BUTTON_X;
    private static final int DIGGING_MODE_Y = ROW2_Y;
    private static final int DIGGING_MODE_W = PREVIEW_BUTTON_W;
    private static final int DISABLED_SLOT_OVERLAY = 0xA0000000;

    private static final ItemStack GHOST_DRONE = new ItemStack(ModItems.DRONE.get());
    private static final ItemStack GHOST_DRILL = new ItemStack(ModItems.DRILL_DIAMOND.get());
    private static final ItemStack GHOST_DIGGER = new ItemStack(ModItems.MODULE_DIGGER.get());
    private static final ItemStack GHOST_SPEED = new ItemStack(ModItems.MODULE_SPEED.get());
    private static final List<ItemStack> ENCHANT_GHOST_STACKS = QuarryModules.enchantGhostStacks();

    private final GuiCycleTimer enchantGhostCycle = new GuiCycleTimer(() -> 1000);

    private Button closeButton;
    private Button rebootButton;
    private Button buttonUp;
    private Button buttonLeft;
    private Button buttonRight;
    private Button buttonDepth;
    private Button buttonPreview;
    private Button diggingModeButton;
    private ItemIconButton redstoneButton;
    private boolean previewButtonShowsHide;

    public QuarryScreen(QuarryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = QuarryMenu.GUI_WIDTH;
        this.imageHeight = QuarryMenu.GUI_HEIGHT;
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
        buttonUp = Button.builder(Component.literal("\u2191"), b -> sendSize(0, true))
                .bounds(upX, topPos + ROW1_Y, BUTTON_W, BUTTON_H).build();
        buttonLeft = Button.builder(Component.literal("\u2190"), b -> sendSize(1, true))
                .bounds(leftPos + ARROW_GROUP_LEFT_X, topPos + ROW2_Y, BUTTON_W, BUTTON_H).build();
        buttonDepth = Button.builder(Component.literal("-"), b -> sendSize(3, true))
                .bounds(leftPos + ARROW_GROUP_LEFT_X + BUTTON_W + GAP, topPos + ROW2_Y, BUTTON_W, BUTTON_H).build();
        buttonRight = Button.builder(Component.literal("\u2192"), b -> sendSize(2, true))
                .bounds(leftPos + ARROW_GROUP_LEFT_X + 2 * (BUTTON_W + GAP), topPos + ROW2_Y, BUTTON_W, BUTTON_H).build();
        addRenderableWidget(buttonUp);
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

        redstoneButton = addRenderableWidget(MachineGuiButtons.redstoneIconButton(
                leftPos + REDSTONE_X, topPos + REDSTONE_Y, b -> sendRedstone(false), menu::getRedstoneMode, false));

        updateButtonTooltips();
        previewButtonShowsHide = menu.isPreviewEnabled();
        updatePreviewButtonLabel();
        updateDiggingModeButton();
        if (menu.isPreviewEnabled()) {
            BlockPos pos = menu.getSyncedBlockPos();
            if (!pos.equals(BlockPos.ZERO)) {
                PacketDistributor.sendToServer(new QuarryPreviewToggleC2SPacket(pos, true));
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

    private int modifierStepAmount() {
        if (minecraft == null || minecraft.getWindow() == null) {
            return 1;
        }
        var window = minecraft.getWindow().getWindow();
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
        PacketDistributor.sendToServer(new QuarrySizeC2SPacket(pos, direction, increment, modifierStepAmount()));
        playButtonSound();
    }

    private void togglePreview() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        playButtonSound();
        boolean enabling = !menu.isPreviewEnabled();
        MarkRenderer.getInstance().clearBillboardMarkersForOwner(pos);
        PacketDistributor.sendToServer(new QuarryPreviewToggleC2SPacket(pos, enabling));
        previewButtonShowsHide = enabling;
        updatePreviewButtonLabel();
    }

    private void sendReboot() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        PacketDistributor.sendToServer(new QuarryRebootC2SPacket(pos));
        playButtonSound();
    }

    private void sendDiggingModeToggle() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        PacketDistributor.sendToServer(new QuarryDiggingModeC2SPacket(pos));
        playButtonSound();
    }

    private void sendRedstone(boolean backward) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            PacketDistributor.sendToServer(new QuarryRedstoneModeC2SPacket(pos, backward));
            playButtonSound();
        }
    }

    private void playButtonSound() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight,
                QuarryMenu.GUI_WIDTH, QuarryMenu.GUI_HEIGHT);
        renderGhostItems(guiGraphics);
        renderDisabledEquipmentSlots(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderEnergyBar(guiGraphics);
        renderAreaLabel(guiGraphics);
        renderChunksProgressLabel(guiGraphics);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        renderGhostTooltips(guiGraphics, mouseX, mouseY);
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        if (redstoneButton != null && redstoneButton.isMouseOver(mouseX, mouseY)) {
            MachineGuiButtons.renderTooltipLine(guiGraphics, font, mouseX, mouseY,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), false));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleWidth = this.font.width(this.title);
        int titleX = (this.imageWidth - titleWidth) / 2;
        guiGraphics.drawString(this.font, this.title, titleX, TITLE_Y, 0x404040, false);
    }

    private void drawCenteredText(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
        guiGraphics.drawString(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
    }

    private void renderDisabledEquipmentSlots(GuiGraphics guiGraphics) {
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

    private void renderDisabledSlotOverlay(GuiGraphics guiGraphics, int slotX, int slotY) {
        int x = this.leftPos + slotX;
        int y = this.topPos + slotY;
        guiGraphics.fill(x, y, x + 16, y + 16, DISABLED_SLOT_OVERLAY);
    }

    private void renderAreaLabel(GuiGraphics guiGraphics) {
        int labelCenterX = this.leftPos + ARROW_GROUP_CENTER_X;
        Component sizeLabel = Component.translatable(
                "gui.another_quarries.quarry.size",
                menu.getSizeLeft(),
                menu.getSizeRight(),
                menu.getAreaWidth(),
                menu.getAreaHeight(),
                menu.getAreaDepth());
        drawCenteredText(guiGraphics, sizeLabel, labelCenterX, this.topPos + SIZE_LABEL_Y, 0x404040);
    }

    private void renderChunksProgressLabel(GuiGraphics guiGraphics) {
        int total = menu.getTotalAreaChunkCount();
        if (total <= 1) {
            return;
        }
        int centerX = this.leftPos + this.imageWidth / 2;
        Component label = Component.translatable(
                "gui.another_quarries.quarry.chunks_progress",
                menu.getProcessedAreaChunkCount(),
                total);
        drawCenteredText(guiGraphics, label, centerX, this.topPos + CHUNKS_LABEL_Y, 0x404040);
    }

    private void renderGhostItems(GuiGraphics guiGraphics) {
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

    private void renderEnergyBar(GuiGraphics guiGraphics) {
        int energy = menu.getEnergyStored();
        int maxEnergy = Math.max(1, menu.getMaxEnergyStored());
        int barX = leftPos + ENERGY_BAR_X;
        int barY = topPos + ENERGY_BAR_Y;
        guiGraphics.blit(ENERGY_BAR_TEXTURE, barX, barY, 8, 0, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, 16, 32);
        if (energy > 0) {
            int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
            int energyY = barY + (ENERGY_BAR_HEIGHT - energyHeight);
            guiGraphics.blit(ENERGY_BAR_TEXTURE, barX, energyY, 0, ENERGY_BAR_HEIGHT - energyHeight,
                    ENERGY_BAR_WIDTH, energyHeight, 16, 32);
        }
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
            guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    private void renderGhostTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
                guiGraphics.renderTooltip(this.font, ghost, mouseX, mouseY);
                return;
            }
            ItemStack ghost = ghostForEquipmentSlot(equipmentSlot);
            if (!ghost.isEmpty()) {
                guiGraphics.renderTooltip(this.font, ghost, mouseX, mouseY);
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
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            if (buttonUp != null && buttonUp.isMouseOver(mouseX, mouseY)) {
                sendSize(0, false);
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
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
