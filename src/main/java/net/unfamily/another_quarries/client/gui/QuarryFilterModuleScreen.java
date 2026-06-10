package net.unfamily.another_quarries.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.another_quarries.config.ModConfig;
import net.unfamily.another_quarries.integration.anotherdynamics.AnotherDynamicsIntegration;
import net.unfamily.another_quarries.integration.anotherdynamics.QuarryFilterCopierCompat;
import net.unfamily.another_quarries.integration.jei.JeiRuntimeState;
import net.unfamily.another_quarries.integration.jei.ghost.IQuarryGhostTarget;
import org.jetbrains.annotations.Nullable;
import net.unfamily.another_quarries.network.packet.QuarryFilterCopyToCopierC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryFilterLineUpdateC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryFilterPasteFromCopierC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryFilterRequestSyncC2SPacket;

import java.util.ArrayList;
import java.util.List;

public class QuarryFilterModuleScreen extends AbstractContainerScreen<QuarryFilterModuleMenu> implements IQuarryGhostTarget {
    private enum SubView {
        DESTROY_FILTERS,
        FILTER_HELP
    }

    private static final int CLOSE_BUTTON_SIZE = QuarryFilterGuiLayout.CLOSE_BUTTON_SIZE;
    private static final int CLOSE_BUTTON_Y = QuarryFilterGuiLayout.CLOSE_BUTTON_Y;
    private static final int TITLE_Y = QuarryFilterGuiLayout.TITLE_Y;

    private SubView subView = SubView.DESTROY_FILTERS;
    private SubView filterListBeforeHelp = SubView.DESTROY_FILTERS;
    private int filterScrollOffset;
    private int filterEditIndex = -1;
    private String filterEditOriginal = "";
    private final List<Button> filterRowEditButtons = new ArrayList<>();
    private final List<Button> filterRowClearButtons = new ArrayList<>();
    private EditBox filterEditTextBox;
    private Button filterEditLeftArrow;
    private Button filterEditRightArrow;
    private Button filterEditApplyButton;
    private Button filterEditClearButton;
    private Button filterEditCloseButton;
    private int filterEditGhostSlotX;
    private int filterEditGhostSlotY;
    private ItemStack ghostSlotItem = ItemStack.EMPTY;

    private final QuarryFilterScrollbarInput filterScrollbar = new QuarryFilterScrollbarInput();
    private List<String> filterVariants = List.of();
    private int currentFilterVariantIndex;

    private Button closeButton;
    private Button filterHelpButton;
    private Button filterHelpBackButton;
    private Button settingsCopierSaveButton;
    private Button settingsCopierLoadButton;
    private final QuarryFilterHelpRenderer.Frame filterHelpFrame = new QuarryFilterHelpRenderer.Frame();

    public QuarryFilterModuleScreen(QuarryFilterModuleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = QuarryFilterGuiLayout.PANEL_WIDTH;
        this.imageHeight = QuarryFilterGuiLayout.PANEL_HEIGHT;
        this.inventoryLabelY = 10000;
    }

    private boolean showsCopierColumn() {
        return AnotherDynamicsIntegration.isLoaded();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        closeButton = Button.builder(Component.literal("\u2715"), b -> closeButtonAction())
                .bounds(leftPos + QuarryFilterGuiLayout.CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y,
                        CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).build();
        addRenderableWidget(closeButton);

        filterHelpButton = Button.builder(Component.translatable("gui.another_quarries.quarry.filter.how_to_use"), b -> showFilterHelpSubview())
                .bounds(0, 0, QuarryFilterGuiLayout.ADVANCED_FILTER_BUTTON_WIDTH, QuarryFilterGuiLayout.BTN_H).build();
        filterHelpButton.visible = false;
        addRenderableWidget(filterHelpButton);

        filterHelpBackButton = Button.builder(Component.translatable("gui.another_quarries.quarry.filter.help.back"), b -> closeFilterHelpSubview())
                .bounds(0, 0, QuarryFilterGuiLayout.ADVANCED_FILTER_BUTTON_WIDTH, QuarryFilterGuiLayout.BTN_H).build();
        filterHelpBackButton.visible = false;
        addRenderableWidget(filterHelpBackButton);

        settingsCopierSaveButton = Button.builder(Component.translatable("gui.another_quarries.quarry.filter.copier.copy"), b -> sendFilterCopyToCopier())
                .bounds(0, 0, 18, QuarryFilterGuiLayout.COPIER_ACTION_BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("gui.another_quarries.quarry.filter.copier.copy.tooltip")))
                .build();
        settingsCopierSaveButton.visible = false;
        addRenderableWidget(settingsCopierSaveButton);

        settingsCopierLoadButton = Button.builder(Component.translatable("gui.another_quarries.quarry.filter.copier.paste"), b -> sendFilterPasteFromCopier())
                .bounds(0, 0, 18, QuarryFilterGuiLayout.COPIER_ACTION_BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("gui.another_quarries.quarry.filter.copier.paste.tooltip")))
                .build();
        settingsCopierLoadButton.visible = false;
        addRenderableWidget(settingsCopierLoadButton);

        filterEditLeftArrow = Button.builder(Component.literal("\u2190"), b -> cycleFilterVariant(-1))
                .bounds(0, 0, QuarryFilterGuiLayout.ROW_BUTTON_SIZE, QuarryFilterGuiLayout.ROW_BUTTON_SIZE).build();
        filterEditLeftArrow.visible = false;
        addRenderableWidget(filterEditLeftArrow);

        filterEditRightArrow = Button.builder(Component.literal("\u2192"), b -> cycleFilterVariant(1))
                .bounds(0, 0, QuarryFilterGuiLayout.ROW_BUTTON_SIZE, QuarryFilterGuiLayout.ROW_BUTTON_SIZE).build();
        filterEditRightArrow.visible = false;
        addRenderableWidget(filterEditRightArrow);

        filterEditTextBox = new EditBox(font, 0, 0, 1, 15, Component.empty());
        filterEditTextBox.setMaxLength(256);
        filterEditTextBox.setEditable(true);
        applyFilterEntryEditBoxTextStyle();
        filterEditTextBox.visible = false;
        addRenderableWidget(filterEditTextBox);

        filterEditClearButton = Button.builder(Component.literal("C"), b -> clearFilterEditLine())
                .bounds(0, 0, QuarryFilterGuiLayout.ROW_BUTTON_SIZE, QuarryFilterGuiLayout.ROW_BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("gui.another_quarries.quarry.filter.clear")))
                .build();
        filterEditClearButton.visible = false;
        addRenderableWidget(filterEditClearButton);

        filterEditApplyButton = Button.builder(Component.literal("A"), b -> applyFilterEdit())
                .bounds(0, 0, QuarryFilterGuiLayout.ROW_BUTTON_SIZE, QuarryFilterGuiLayout.ROW_BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("gui.another_quarries.quarry.filter.apply")))
                .build();
        filterEditApplyButton.visible = false;
        addRenderableWidget(filterEditApplyButton);

        filterEditCloseButton = Button.builder(Component.literal("\u2715"), b -> exitFilterEditMode(true))
                .bounds(0, 0, QuarryFilterGuiLayout.ROW_BUTTON_SIZE, QuarryFilterGuiLayout.ROW_BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("gui.another_quarries.quarry.filter.close_without_saving")))
                .build();
        filterEditCloseButton.visible = false;
        addRenderableWidget(filterEditCloseButton);

        QuarryFilterModuleClientSync.consumePending(menu);
        layoutFilterWidgets();
        updateSubviewVisibility();
        rebuildFilterEntryWidgets();
        if (inFilterEditMode()) {
            reloadFilterEditTextBoxFromList(false);
            layoutFilterEditWidgets();
            updateSubviewVisibility();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        QuarryFilterModuleClientSync.consumePending(menu);
        if (menu.consumeClientDestroyFiltersDirty() && subView == SubView.DESTROY_FILTERS) {
            rebuildFilterEntryWidgets();
        }
        updateCopierPasteButtonState();
        layoutFilterWidgets();
    }

    private InteractionHand editHand() {
        return menu.getEditHand();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (subView == SubView.FILTER_HELP) {
            guiGraphics.blit(QuarryFilterGuiSprites.VALID_KEYS_BACKGROUND, leftPos, topPos, 0.0F, 0.0F,
                    imageWidth, imageHeight, QuarryFilterGuiLayout.PANEL_WIDTH, QuarryFilterGuiLayout.PANEL_HEIGHT);
            return;
        }
        guiGraphics.blit(QuarryFilterGuiSprites.FILTER_BACKGROUND, leftPos, topPos, 0.0F, 0.0F,
                imageWidth, imageHeight, QuarryFilterGuiLayout.PANEL_WIDTH, QuarryFilterGuiLayout.PANEL_HEIGHT);
        renderFilterPanel(guiGraphics, mouseX, mouseY);
        if (inFilterEditMode()) {
            renderFilterEditGhostSlot(guiGraphics);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (subView == SubView.FILTER_HELP) {
            renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            renderBg(guiGraphics, partialTick, mouseX, mouseY);
            for (Renderable renderable : renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(leftPos, topPos, 0.0F);
            renderLabels(guiGraphics, mouseX, mouseY);
            guiGraphics.pose().popPose();
            renderTooltip(guiGraphics, mouseX, mouseY);
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty()) {
                int cx = mouseX - 8;
                int cy = mouseY - 8;
                guiGraphics.renderItem(carried, cx, cy);
                guiGraphics.renderItemDecorations(font, carried, cx, cy);
            }
            return;
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (showsCopierColumn()) {
            renderCopierColumn(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (subView == SubView.FILTER_HELP) {
            Component title = Component.translatable("gui.another_quarries.quarry.filter.how_to_use");
            int titleX = (imageWidth - font.width(title)) / 2;
            guiGraphics.drawString(font, title, titleX, TITLE_Y, QuarryFilterGuiLayout.FILTER_ENTRY_TEXT_COLOR, false);
            QuarryFilterHelpRenderer.render(guiGraphics, font, mouseX - leftPos, mouseY - topPos, filterHelpFrame);
            return;
        }
        Component title = Component.translatable("gui.another_quarries.quarry.filter.title");
        int titleX = (imageWidth - font.width(title)) / 2;
        guiGraphics.drawString(font, title, titleX, TITLE_Y, QuarryFilterGuiLayout.FILTER_ENTRY_TEXT_COLOR, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (subView == SubView.FILTER_HELP) {
            QuarryFilterHelpRenderer.renderExampleTooltip(guiGraphics, font, filterHelpFrame, mouseX, mouseY, leftPos, topPos);
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        renderFilterEntryIconTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (subView == SubView.FILTER_HELP) {
            if (button == 0 && QuarryFilterHelpRenderer.handleExampleClick(
                    filterHelpFrame, (int) mouseX, (int) mouseY, leftPos, topPos, font.lineHeight, example -> {
                        if (minecraft != null && minecraft.keyboardHandler != null) {
                            minecraft.keyboardHandler.setClipboard(example);
                            playButtonSound();
                        }
                    })) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (inFilterEditMode() && button == 0) {
            if (mouseX >= filterEditGhostSlotX && mouseX < filterEditGhostSlotX + 18
                    && mouseY >= filterEditGhostSlotY && mouseY < filterEditGhostSlotY + 18) {
                handleFilterGhostSlotClick();
                return true;
            }
        }
        if (subView == SubView.DESTROY_FILTERS && !inFilterEditMode()) {
            int maxLines = ModConfig.quarryFilterMaxLines();
            if (filterScrollbar.handleScrollButtonClick(mouseX, mouseY, leftPos, topPos, maxLines, filterScrollOffset,
                    this::setFilterScrollOffsetWithSound)) {
                return true;
            }
            if (filterScrollbar.handleHandleClick(mouseX, mouseY, leftPos, topPos, maxLines, filterScrollOffset)) {
                return true;
            }
            if (filterScrollbar.handleTrackClick(mouseX, mouseY, leftPos, topPos, maxLines, filterScrollOffset,
                    this::setFilterScrollOffsetWithSound)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (subView == SubView.DESTROY_FILTERS && !inFilterEditMode()) {
            int maxLines = ModConfig.quarryFilterMaxLines();
            if (filterScrollbar.handleDrag(mouseY, maxLines, this::setFilterScrollOffset)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        filterScrollbar.stopDragging();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (subView == SubView.DESTROY_FILTERS && !inFilterEditMode()) {
            int maxLines = ModConfig.quarryFilterMaxLines();
            if (filterScrollbar.handleWheel(scrollY, maxLines, filterScrollOffset, this::setFilterScrollOffset)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (subView == SubView.FILTER_HELP) {
                closeFilterHelpSubview();
                return true;
            }
            if (inFilterEditMode()) {
                exitFilterEditMode(true);
                return true;
            }
        }

        if (inFilterEditMode() && filterEditTextBox != null) {
            if (JeiRuntimeState.jeiHasKeyboardFocusOrRecipesGuiOpen()) {
                return false;
            }
            if (QuarryFilterGuiInput.handleFilterEditKeyPressed(
                    this, keyCode, scanCode, modifiers, filterEditTextBox, this::applyFilterEdit)) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (inFilterEditMode() && filterEditTextBox != null
                && QuarryFilterGuiInput.handleFilterEditCharTyped(filterEditTextBox, codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void closeButtonAction() {
        if (subView == SubView.FILTER_HELP) {
            closeFilterHelpSubview();
            return;
        }
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
    }

    private void showFilterHelpSubview() {
        unfocusAllTextFields();
        filterListBeforeHelp = subView;
        subView = SubView.FILTER_HELP;
        layoutFilterWidgets();
        updateSubviewVisibility();
        playButtonSound();
    }

    private void closeFilterHelpSubview() {
        subView = filterListBeforeHelp;
        rebuildFilterEntryWidgets();
        layoutFilterWidgets();
        updateSubviewVisibility();
        playButtonSound();
    }

    private void unfocusAllTextFields() {
        if (filterEditTextBox != null) {
            filterEditTextBox.setFocused(false);
        }
    }

    private void updateSubviewVisibility() {
        boolean destroyFilters = subView == SubView.DESTROY_FILTERS;
        boolean help = subView == SubView.FILTER_HELP;
        boolean showEditChrome = inFilterEditMode() && !help;
        setVisible(closeButton, true);
        setVisible(filterHelpButton, destroyFilters && !help);
        boolean copierUi = showsCopierColumn() && !help;
        setVisible(settingsCopierSaveButton, copierUi);
        setVisible(settingsCopierLoadButton, copierUi);
        boolean showRowButtons = destroyFilters && !help;
        for (Button b : filterRowEditButtons) setVisible(b, showRowButtons);
        for (Button b : filterRowClearButtons) setVisible(b, showRowButtons);
        setVisible(filterEditTextBox, showEditChrome);
        setVisible(filterEditLeftArrow, showEditChrome);
        setVisible(filterEditRightArrow, showEditChrome);
        setVisible(filterEditApplyButton, showEditChrome);
        setVisible(filterEditClearButton, showEditChrome);
        setVisible(filterEditCloseButton, showEditChrome);
        setVisible(filterHelpBackButton, help);
        menu.setInventoryInteractionBlocked(help);
        updateCopierPasteButtonState();
        layoutFilterWidgets();
    }

    private void updateCopierPasteButtonState() {
        if (settingsCopierLoadButton == null || minecraft == null || minecraft.player == null) return;
        if (!settingsCopierLoadButton.visible) return;
        boolean filterListMode = QuarryFilterCopierCompat.isHeldCopierFilterListMode(minecraft.player);
        settingsCopierLoadButton.active = filterListMode;
        settingsCopierLoadButton.setTooltip(Tooltip.create(Component.translatable(
                filterListMode
                        ? "gui.another_quarries.quarry.filter.copier.paste.tooltip"
                        : "gui.another_quarries.quarry.filter.copier.paste.disabled")));
    }

    private static void setVisible(AbstractWidget widget, boolean visible) {
        if (widget != null) {
            widget.visible = visible;
            widget.active = visible;
        }
    }

    private void requestFilterSync() {
        PacketDistributor.sendToServer(new QuarryFilterRequestSyncC2SPacket(editHand()));
    }

    private void sendFilterLineUpdate(int index, String text) {
        menu.setClientDestroyFilterLine(index, text);
        PacketDistributor.sendToServer(new QuarryFilterLineUpdateC2SPacket(editHand(), index, text));
    }

    private void sendFilterCopyToCopier() {
        PacketDistributor.sendToServer(new QuarryFilterCopyToCopierC2SPacket(editHand()));
        playButtonSound();
    }

    private void sendFilterPasteFromCopier() {
        PacketDistributor.sendToServer(new QuarryFilterPasteFromCopierC2SPacket(editHand()));
        playButtonSound();
    }

    private void setFilterScrollOffset(int offset) {
        int newOffset = Mth.clamp(offset, 0, maxFilterScroll());
        if (newOffset == filterScrollOffset) {
            return;
        }
        filterScrollOffset = newOffset;
        if (inFilterEditMode()) {
            int row = filterEditIndex - filterScrollOffset;
            if (row < 0 || row >= QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES) {
                exitFilterEditMode(true);
            }
        }
        rebuildFilterEntryWidgets();
    }

    private void setFilterScrollOffsetWithSound(int offset) {
        int before = filterScrollOffset;
        setFilterScrollOffset(offset);
        if (filterScrollOffset != before) {
            playButtonSound();
        }
    }

    private int maxFilterScroll() {
        return Math.max(0, ModConfig.quarryFilterMaxLines() - QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES);
    }

    private void rebuildFilterEntryWidgets() {
        clearFilterEntryButtons();
        if (subView != SubView.DESTROY_FILTERS) return;
        ensureEditableFilterLines();
        int maxLines = ModConfig.quarryFilterMaxLines();
        for (int row = 0; row < QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES; row++) {
            int index = filterScrollOffset + row;
            if (index >= maxLines) break;
            int entryX = leftPos + QuarryFilterGuiLayout.ENTRY_X;
            int entryY = topPos + QuarryFilterGuiLayout.FIRST_FILTER_ROW_Y + row * QuarryFilterGuiLayout.ENTRY_HEIGHT;
            int buttonSize = QuarryFilterGuiLayout.ROW_BUTTON_SIZE;
            int buttonMargin = QuarryFilterGuiLayout.ROW_BUTTON_MARGIN;
            int editX = entryX + QuarryFilterGuiLayout.ENTRY_WIDTH - buttonMargin - buttonSize;
            int clearX = editX - buttonSize - QuarryFilterGuiLayout.ROW_BUTTON_SPACING;
            int buttonY = entryY + (QuarryFilterGuiLayout.ENTRY_HEIGHT - buttonSize) / 2;
            final int lineIndex = index;
            Button clearBtn = Button.builder(Component.literal("C"), b -> clearFilterLine(lineIndex))
                    .bounds(clearX, buttonY, buttonSize, buttonSize)
                    .tooltip(Tooltip.create(Component.translatable("gui.another_quarries.quarry.filter.clear")))
                    .build();
            filterRowClearButtons.add(clearBtn);
            addRenderableWidget(clearBtn);
            Button editBtn = Button.builder(Component.literal("\u270e"), b -> enterFilterEditMode(lineIndex))
                    .bounds(editX, buttonY, buttonSize, buttonSize).build();
            filterRowEditButtons.add(editBtn);
            addRenderableWidget(editBtn);
        }
        layoutFilterWidgets();
        updateSubviewVisibility();
    }

    private void clearFilterEntryButtons() {
        for (Button b : filterRowEditButtons) removeWidget(b);
        filterRowEditButtons.clear();
        for (Button b : filterRowClearButtons) removeWidget(b);
        filterRowClearButtons.clear();
    }

    private void ensureEditableFilterLines() {
        int max = ModConfig.quarryFilterMaxLines();
        List<String> lines = menu.getClientDestroyFilters();
        while (lines.size() < max && (lines.isEmpty() || !lines.getLast().isEmpty())) lines.add("");
    }

    private boolean inFilterEditMode() { return filterEditIndex >= 0; }

    private void enterFilterEditMode(int index) {
        if (filterEditIndex == index) { exitFilterEditMode(true); return; }
        exitFilterEditMode(false);
        filterEditIndex = index;
        ensureEditableFilterLines();
        List<String> lines = menu.getClientDestroyFilters();
        while (lines.size() <= index) lines.add("");
        filterEditOriginal = lines.get(index) != null ? lines.get(index) : "";
        filterVariants = QuarryFilterVariants.variantsForLine(filterEditOriginal);
        currentFilterVariantIndex = filterVariants.indexOf(filterEditOriginal.trim());
        if (currentFilterVariantIndex < 0) {
            currentFilterVariantIndex = 0;
        }
        if (filterEditTextBox != null) {
            filterEditTextBox.setValue(filterEditOriginal);
            filterEditTextBox.setCursorPosition(0);
            filterEditTextBox.setHighlightPos(0);
            filterEditTextBox.setFocused(true);
            applyFilterEntryEditBoxTextStyle();
        }
        filterScrollOffset = Mth.clamp(filterScrollOffset, 0, maxFilterScroll());
        layoutFilterWidgets();
        updateSubviewVisibility();
        rebuildFilterEntryWidgets();
        playButtonSound();
    }

    private void exitFilterEditMode(boolean restoreValue) {
        if (filterEditIndex >= 0 && restoreValue) sendFilterLineUpdate(filterEditIndex, filterEditOriginal);
        filterEditIndex = -1;
        filterEditOriginal = "";
        filterVariants = List.of();
        currentFilterVariantIndex = 0;
        clearGhostCalibration(false);
        if (filterEditTextBox != null) filterEditTextBox.setFocused(false);
        layoutFilterWidgets();
        updateSubviewVisibility();
        if (subView == SubView.DESTROY_FILTERS) rebuildFilterEntryWidgets();
    }

    private void reloadFilterEditTextBoxFromList(boolean grabFocus) {
        if (!inFilterEditMode() || filterEditTextBox == null) {
            return;
        }
        ensureEditableFilterLines();
        List<String> lines = menu.getClientDestroyFilters();
        while (lines.size() <= filterEditIndex) {
            lines.add("");
        }
        filterEditOriginal = lines.get(filterEditIndex) != null ? lines.get(filterEditIndex) : "";
        filterVariants = QuarryFilterVariants.variantsForLine(filterEditOriginal);
        currentFilterVariantIndex = filterVariants.indexOf(filterEditOriginal.trim());
        if (currentFilterVariantIndex < 0) {
            currentFilterVariantIndex = 0;
        }
        filterEditTextBox.setValue(filterEditOriginal);
        filterEditTextBox.setCursorPosition(0);
        filterEditTextBox.setHighlightPos(0);
        applyFilterEntryEditBoxTextStyle();
        filterEditTextBox.setFocused(grabFocus);
    }

    private void applyFilterEntryEditBoxTextStyle() {
        if (filterEditTextBox != null) {
            filterEditTextBox.setTextColor(QuarryFilterGuiLayout.FILTER_EDIT_TEXT_COLOR);
        }
    }

    private void cycleFilterVariant(int direction) {
        if (!inFilterEditMode() || filterEditTextBox == null) {
            return;
        }
        if (filterVariants.isEmpty()) {
            filterVariants = QuarryFilterVariants.variantsForLine(filterEditTextBox.getValue());
            currentFilterVariantIndex = filterVariants.indexOf(filterEditTextBox.getValue().trim());
            if (currentFilterVariantIndex < 0) {
                currentFilterVariantIndex = 0;
            }
        }
        String next = QuarryFilterVariants.cycle(filterVariants, filterEditTextBox.getValue(), direction);
        currentFilterVariantIndex = filterVariants.indexOf(next);
        filterEditTextBox.setValue(next);
        filterEditTextBox.setCursorPosition(next.length());
        filterEditTextBox.setHighlightPos(next.length());
        playButtonSound();
    }

    private void handleGhostIngredientDrop(ItemStack stack) {
        applyGhostFromItemStack(stack, true);
        playButtonSound();
    }

    private void clearGhostCalibration(boolean updateEditBox) {
        ghostSlotItem = ItemStack.EMPTY;
        filterVariants = List.of();
        currentFilterVariantIndex = 0;
        if (updateEditBox && filterEditTextBox != null) {
            filterEditTextBox.setValue("");
            filterEditTextBox.setCursorPosition(0);
            filterEditTextBox.setHighlightPos(0);
        }
    }

    private void applyGhostFromItemStack(ItemStack stack, boolean updateEditBox) {
        if (stack.isEmpty()) {
            clearGhostCalibration(updateEditBox);
            return;
        }
        ghostSlotItem = stack.copy();
        filterVariants = QuarryFilterVariants.variantsFromItemStack(stack);
        currentFilterVariantIndex = 0;
        if (filterVariants.isEmpty()) {
            return;
        }
        if (updateEditBox && filterEditTextBox != null) {
            String first = filterVariants.get(0);
            filterEditTextBox.setValue(first);
            filterEditTextBox.setCursorPosition(first.length());
            filterEditTextBox.setHighlightPos(first.length());
            sendFilterLineUpdate(filterEditIndex, first);
        }
    }

    private void handleFilterGhostSlotClick() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) {
            clearGhostCalibration(true);
        } else {
            applyGhostFromItemStack(carried, true);
        }
        playButtonSound();
    }

    @Override
    public @Nullable IGhostIngredientConsumer getGhostHandler() {
        if (subView != SubView.DESTROY_FILTERS || !inFilterEditMode()) {
            return null;
        }
        return new IGhostIngredientConsumer() {
            @Override
            public @Nullable Object supportedTarget(Object ingredient) {
                if (ingredient instanceof ItemStack stack && !stack.isEmpty()) {
                    return stack;
                }
                return null;
            }

            @Override
            public void accept(Object ingredient) {
                if (ingredient instanceof ItemStack stack) {
                    handleGhostIngredientDrop(stack);
                }
            }
        };
    }

    @Override
    public @Nullable Rect2i getGhostTargetArea() {
        if (!inFilterEditMode()) {
            return null;
        }
        return new Rect2i(filterEditGhostSlotX, filterEditGhostSlotY, 18, 18);
    }

    private void applyFilterEdit() {
        if (!inFilterEditMode() || filterEditTextBox == null) return;
        sendFilterLineUpdate(filterEditIndex, filterEditTextBox.getValue());
        filterEditOriginal = filterEditTextBox.getValue();
        exitFilterEditMode(false);
        playButtonSound();
    }

    private void clearFilterEditLine() {
        if (!inFilterEditMode()) return;
        clearFilterLine(filterEditIndex);
        if (filterEditTextBox != null) filterEditTextBox.setValue("");
        filterEditOriginal = "";
        playButtonSound();
    }

    private void clearFilterLine(int index) {
        sendFilterLineUpdate(index, "");
        rebuildFilterEntryWidgets();
    }

    private void layoutFilterWidgets() {
        layoutFilterNavButtons();
        layoutHelpBackButton();
        layoutFilterEditWidgets();
        layoutCopierButtons();
    }

    private void layoutHelpBackButton() {
        if (filterHelpBackButton == null || subView != SubView.FILTER_HELP) {
            return;
        }
        filterHelpBackButton.setX(leftPos + QuarryFilterGuiLayout.HELP_BACK_BUTTON_X);
        filterHelpBackButton.setY(topPos + QuarryFilterGuiLayout.HELP_BACK_BUTTON_Y);
        filterHelpBackButton.setWidth(QuarryFilterGuiLayout.ADVANCED_FILTER_BUTTON_WIDTH);
        filterHelpBackButton.setHeight(QuarryFilterGuiLayout.BTN_H);
    }

    private void layoutFilterNavButtons() {
        if (filterHelpButton == null || subView != SubView.DESTROY_FILTERS) {
            return;
        }
        int bx = leftPos + QuarryFilterGuiLayout.navBackButtonX();
        int by = topPos + QuarryFilterGuiLayout.navBackButtonY();
        filterHelpButton.setX(bx);
        filterHelpButton.setY(by);
        filterHelpButton.setWidth(QuarryFilterGuiLayout.ADVANCED_FILTER_BUTTON_WIDTH);
        filterHelpButton.setHeight(QuarryFilterGuiLayout.BTN_H);
    }

    private void layoutCopierButtons() {
        if (settingsCopierSaveButton == null || settingsCopierLoadButton == null) return;
        int colX = leftPos + QuarryFilterGuiLayout.SLOT_COPY_BACKGROUND_X;
        settingsCopierSaveButton.setX(colX);
        settingsCopierSaveButton.setY(topPos + QuarryFilterGuiLayout.COPIER_SAVE_BUTTON_Y);
        settingsCopierLoadButton.setX(colX);
        settingsCopierLoadButton.setY(topPos + QuarryFilterGuiLayout.COPIER_LOAD_BUTTON_Y);
    }

    private void layoutFilterEditWidgets() {
        if (filterEditTextBox == null) return;
        int slotSize = 18;
        int buttonSize = QuarryFilterGuiLayout.ROW_BUTTON_SIZE;
        int buttonSpacing = QuarryFilterGuiLayout.ROW_BUTTON_SPACING;
        int slotY = topPos + QuarryFilterGuiLayout.editModeRowAnchorY();
        int rowLeft = leftPos + QuarryFilterGuiLayout.ENTRY_X;
        int rowRight = leftPos + QuarryFilterGuiLayout.ENTRY_X + QuarryFilterGuiLayout.ENTRY_WIDTH;
        int leftArrowX = rowLeft;
        int slotX = rowLeft + buttonSize + buttonSpacing;
        int rightArrowX = slotX + slotSize + buttonSpacing;
        int closeButtonX = rowRight - buttonSize;
        int applyButtonX = closeButtonX - buttonSize - buttonSpacing;
        int clearButtonX = applyButtonX - buttonSize - buttonSpacing;
        int buttonRowY = slotY + (slotSize - buttonSize) / 2;
        filterEditGhostSlotX = slotX;
        filterEditGhostSlotY = slotY;
        if (filterEditLeftArrow != null) filterEditLeftArrow.setPosition(leftArrowX, buttonRowY);
        if (filterEditRightArrow != null) filterEditRightArrow.setPosition(rightArrowX, buttonRowY);
        if (filterEditClearButton != null) filterEditClearButton.setPosition(clearButtonX, buttonRowY);
        if (filterEditApplyButton != null) filterEditApplyButton.setPosition(applyButtonX, buttonRowY);
        if (filterEditCloseButton != null) filterEditCloseButton.setPosition(closeButtonX, buttonRowY);
        int textBoxY = slotY + slotSize + 2;
        int textBoxX = leftPos + QuarryFilterGuiLayout.ENTRY_X + QuarryFilterGuiLayout.EDIT_MODE_TEXT_INSET_X;
        int entryContentRight = leftPos + QuarryFilterGuiLayout.ENTRY_X + QuarryFilterGuiLayout.ENTRY_WIDTH
                - QuarryFilterGuiLayout.EDIT_MODE_TEXT_INSET_X;
        filterEditTextBox.setPosition(textBoxX, textBoxY);
        filterEditTextBox.setWidth(entryContentRight - textBoxX);
        filterEditTextBox.setHeight(15);
        applyFilterEntryEditBoxTextStyle();
    }

    private void renderFilterPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int maxLines = ModConfig.quarryFilterMaxLines();
        List<String> lines = menu.getClientDestroyFilters();
        for (int row = 0; row < QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES; row++) {
            int index = filterScrollOffset + row;
            if (index >= maxLines) break;
            int entryX = leftPos + QuarryFilterGuiLayout.ENTRY_X;
            int entryY = topPos + QuarryFilterGuiLayout.FIRST_FILTER_ROW_Y + row * QuarryFilterGuiLayout.ENTRY_HEIGHT;
            QuarryFilterGuiSprites.blitEntryRow(guiGraphics, entryX, entryY);
            int slotX = entryX + QuarryFilterGuiLayout.ENTRY_SLOT_INSET;
            int slotY = entryY + QuarryFilterGuiLayout.ENTRY_SLOT_INSET;
            QuarryFilterGuiSprites.blitSingleSlot(guiGraphics, slotX, slotY);
            String filter = index < lines.size() && lines.get(index) != null ? lines.get(index) : "";
            renderFilterRowIcon(guiGraphics, filter, slotX, slotY);
            int textX = slotX + 18 + 6;
            int textY = entryY + (QuarryFilterGuiLayout.ENTRY_HEIGHT - font.lineHeight) / 2;
            int editButtonX = entryX + QuarryFilterGuiLayout.ENTRY_WIDTH - QuarryFilterGuiLayout.ROW_BUTTON_MARGIN
                    - QuarryFilterGuiLayout.ROW_BUTTON_SIZE;
            int clearButtonX = editButtonX - QuarryFilterGuiLayout.ROW_BUTTON_SIZE - QuarryFilterGuiLayout.ROW_BUTTON_SPACING;
            int maxTextWidth = clearButtonX - textX - 5;
            String displayText = filter;
            if (font.width(displayText) > maxTextWidth && !displayText.isEmpty()) {
                displayText = font.plainSubstrByWidth(displayText, maxTextWidth - font.width("...")) + "...";
            }
            guiGraphics.drawString(font, Component.literal(displayText), textX, textY,
                    QuarryFilterGuiLayout.FILTER_ENTRY_TEXT_COLOR, false);
        }
        if (maxLines > QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES) {
            int scrollbarX = leftPos + QuarryFilterGuiLayout.SCROLLBAR_X_REL;
            int buttonUpY = topPos + QuarryFilterGuiLayout.BUTTON_UP_Y_REL;
            int scrollbarY = topPos + QuarryFilterGuiLayout.SCROLLBAR_Y_REL;
            int buttonDownY = topPos + QuarryFilterGuiLayout.BUTTON_DOWN_Y_REL;
            guiGraphics.blit(QuarryFilterGuiSprites.SCROLLBAR, scrollbarX, scrollbarY, 0.0F, 0.0F,
                    QuarryFilterGuiLayout.SCROLLBAR_WIDTH, QuarryFilterGuiLayout.SCROLLBAR_HEIGHT,
                    QuarryFilterGuiLayout.SCROLLBAR_TEXTURE_WIDTH, QuarryFilterGuiLayout.SCROLLBAR_TEXTURE_HEIGHT);
            int upV = mouseX >= scrollbarX && mouseX < scrollbarX + QuarryFilterGuiLayout.SCROLLBAR_WIDTH
                    && mouseY >= buttonUpY && mouseY < buttonUpY + QuarryFilterGuiLayout.HANDLE_SIZE ? QuarryFilterGuiLayout.HANDLE_SIZE : 0;
            guiGraphics.blit(QuarryFilterGuiSprites.SCROLLBAR, scrollbarX, buttonUpY,
                    (float) (QuarryFilterGuiLayout.SCROLLBAR_WIDTH * 2), (float) upV,
                    QuarryFilterGuiLayout.HANDLE_SIZE, QuarryFilterGuiLayout.HANDLE_SIZE,
                    QuarryFilterGuiLayout.SCROLLBAR_TEXTURE_WIDTH, QuarryFilterGuiLayout.SCROLLBAR_TEXTURE_HEIGHT);
            int downV = mouseX >= scrollbarX && mouseX < scrollbarX + QuarryFilterGuiLayout.SCROLLBAR_WIDTH
                    && mouseY >= buttonDownY && mouseY < buttonDownY + QuarryFilterGuiLayout.HANDLE_SIZE ? QuarryFilterGuiLayout.HANDLE_SIZE : 0;
            guiGraphics.blit(QuarryFilterGuiSprites.SCROLLBAR, scrollbarX, buttonDownY,
                    (float) (QuarryFilterGuiLayout.SCROLLBAR_WIDTH * 3), (float) downV,
                    QuarryFilterGuiLayout.HANDLE_SIZE, QuarryFilterGuiLayout.HANDLE_SIZE,
                    QuarryFilterGuiLayout.SCROLLBAR_TEXTURE_WIDTH, QuarryFilterGuiLayout.SCROLLBAR_TEXTURE_HEIGHT);
            int maxScroll = maxFilterScroll();
            if (maxScroll > 0) {
                double ratio = (double) filterScrollOffset / maxScroll;
                int handleY = scrollbarY + (int) (ratio * (QuarryFilterGuiLayout.SCROLLBAR_HEIGHT - QuarryFilterGuiLayout.HANDLE_SIZE));
                int hV = mouseX >= scrollbarX && mouseX < scrollbarX + QuarryFilterGuiLayout.HANDLE_SIZE
                        && mouseY >= handleY && mouseY < handleY + QuarryFilterGuiLayout.HANDLE_SIZE ? QuarryFilterGuiLayout.HANDLE_SIZE : 0;
                guiGraphics.blit(QuarryFilterGuiSprites.SCROLLBAR, scrollbarX, handleY,
                        (float) QuarryFilterGuiLayout.SCROLLBAR_WIDTH, (float) hV,
                        QuarryFilterGuiLayout.HANDLE_SIZE, QuarryFilterGuiLayout.HANDLE_SIZE,
                        QuarryFilterGuiLayout.SCROLLBAR_TEXTURE_WIDTH, QuarryFilterGuiLayout.SCROLLBAR_TEXTURE_HEIGHT);
            }
        }
    }

    private void renderFilterEditGhostSlot(GuiGraphics guiGraphics) {
        QuarryFilterGuiSprites.blitSingleSlot(guiGraphics, filterEditGhostSlotX, filterEditGhostSlotY);
        if (!ghostSlotItem.isEmpty()) {
            guiGraphics.renderItem(ghostSlotItem, filterEditGhostSlotX + 1, filterEditGhostSlotY + 1);
            guiGraphics.renderItemDecorations(font, ghostSlotItem, filterEditGhostSlotX + 1, filterEditGhostSlotY + 1);
            return;
        }
        String line = filterEditTextBox != null ? filterEditTextBox.getValue() : "";
        renderFilterRowIcon(guiGraphics, line, filterEditGhostSlotX, filterEditGhostSlotY);
    }

    private void renderCopierColumn(GuiGraphics guiGraphics) {
        int frameX = leftPos + QuarryFilterGuiLayout.SLOT_COPY_BACKGROUND_X;
        int frameY = topPos + QuarryFilterGuiLayout.SLOT_COPY_BACKGROUND_Y;
        int iconX = leftPos + QuarryFilterGuiLayout.SLOT_COPY_X;
        int iconY = topPos + QuarryFilterGuiLayout.SLOT_COPY_Y;
        QuarryFilterGuiSprites.blitCopierSlotFrame(guiGraphics, frameX, frameY);
        ItemStack copier = resolveCopierDisplayStack();
        if (!copier.isEmpty()) {
            guiGraphics.renderItem(copier, iconX, iconY);
            guiGraphics.renderItemDecorations(font, copier, iconX, iconY);
        }
    }

    private ItemStack resolveCopierDisplayStack() {
        int idx = menu.copySettingsSlotIndex();
        if (idx >= 0) {
            ItemStack inSlot = menu.getSlot(idx).getItem();
            if (!inSlot.isEmpty()) return inSlot;
        }
        if (minecraft != null && minecraft.player != null) {
            ItemStack held = QuarryFilterCopierCompat.findHeldSettingsCopier(minecraft.player);
            if (held != null) return held;
        }
        return ItemStack.EMPTY;
    }

    private void renderFilterRowIcon(GuiGraphics guiGraphics, String filterLine, int slotX, int slotY) {
        ItemStack stack = QuarryFilterRowDisplay.previewStack(filterLine);
        if (!stack.isEmpty()) {
            guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
            guiGraphics.renderItemDecorations(font, stack, slotX + 1, slotY + 1);
        }
    }

    private void renderFilterEntryIconTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (subView != SubView.DESTROY_FILTERS || !menu.getCarried().isEmpty()) {
            return;
        }
        if (inFilterEditMode()) {
            ItemStack ghostStack = !ghostSlotItem.isEmpty()
                    ? ghostSlotItem
                    : QuarryFilterRowDisplay.previewStack(filterEditTextBox != null ? filterEditTextBox.getValue() : "");
            if (!ghostStack.isEmpty()
                    && mouseInRect(mouseX, mouseY, filterEditGhostSlotX, filterEditGhostSlotY, 18, 18)) {
                guiGraphics.renderTooltip(font, ghostStack, mouseX, mouseY);
                return;
            }
        }
        int maxLines = ModConfig.quarryFilterMaxLines();
        List<String> lines = menu.getClientDestroyFilters();
        for (int row = 0; row < QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES; row++) {
            int index = filterScrollOffset + row;
            if (index >= maxLines) {
                break;
            }
            int entryX = leftPos + QuarryFilterGuiLayout.ENTRY_X;
            int entryY = topPos + QuarryFilterGuiLayout.FIRST_FILTER_ROW_Y + row * QuarryFilterGuiLayout.ENTRY_HEIGHT;
            int slotX = entryX + QuarryFilterGuiLayout.ENTRY_SLOT_INSET;
            int slotY = entryY + QuarryFilterGuiLayout.ENTRY_SLOT_INSET;
            String filter = index < lines.size() && lines.get(index) != null ? lines.get(index) : "";
            ItemStack stack = QuarryFilterRowDisplay.previewStack(filter);
            if (!stack.isEmpty() && mouseInRect(mouseX, mouseY, slotX, slotY, 18, 18)) {
                guiGraphics.renderTooltip(font, stack, mouseX, mouseY);
                return;
            }
        }
        if (showsCopierColumn()) {
            int iconX = leftPos + QuarryFilterGuiLayout.SLOT_COPY_X;
            int iconY = topPos + QuarryFilterGuiLayout.SLOT_COPY_Y;
            ItemStack copier = resolveCopierDisplayStack();
            if (!copier.isEmpty() && mouseInRect(mouseX, mouseY, iconX, iconY, 18, 18)) {
                guiGraphics.renderTooltip(font, copier, mouseX, mouseY);
            }
        }
    }

    private static boolean mouseInRect(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void playButtonSound() {
        if (minecraft != null) minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void onClose() {
        if (inFilterEditMode() && filterEditTextBox != null) {
            sendFilterLineUpdate(filterEditIndex, filterEditTextBox.getValue());
        }
        super.onClose();
    }
}
