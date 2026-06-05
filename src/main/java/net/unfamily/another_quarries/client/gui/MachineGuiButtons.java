package net.unfamily.another_quarries.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unfamily.another_quarries.AnotherQuarries;

import java.util.List;
import java.util.function.Supplier;

public final class MachineGuiButtons {
    public static final int ICON_SIZE = 16;
    public static final Identifier REDSTONE_GUI =
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "textures/gui/redstone_gui.png");

    private MachineGuiButtons() {}

    public static int displayRedstoneMode(int mode, boolean allowPulse) {
        if (mode == 3 && !allowPulse) {
            return 4;
        }
        return mode;
    }

    public static ItemStack redstoneIcon(int mode, boolean allowPulse) {
        return switch (displayRedstoneMode(mode, allowPulse)) {
            case 0 -> new ItemStack(Items.GUNPOWDER);
            case 1 -> new ItemStack(Items.REDSTONE);
            case 2 -> ItemStack.EMPTY;
            case 3 -> new ItemStack(Items.REPEATER);
            case 4 -> new ItemStack(Items.BARRIER);
            default -> new ItemStack(Items.REDSTONE);
        };
    }

    public static Identifier redstoneOverlay(int mode, boolean allowPulse) {
        return displayRedstoneMode(mode, allowPulse) == 2 ? REDSTONE_GUI : null;
    }

    public static Component redstoneTooltip(int mode, boolean allowPulse) {
        return switch (displayRedstoneMode(mode, allowPulse)) {
            case 0 -> Component.translatable("gui.another_quarries.generic.redstone_mode.none");
            case 1 -> Component.translatable("gui.another_quarries.generic.redstone_mode.low");
            case 2 -> Component.translatable("gui.another_quarries.generic.redstone_mode.high");
            case 3 -> Component.translatable("gui.another_quarries.generic.redstone_mode.pulse");
            case 4 -> Component.translatable("gui.another_quarries.generic.redstone_mode.disabled");
            default -> Component.literal("Unknown mode");
        };
    }

    public static ItemIconButton redstoneIconButton(
            int x, int y, Button.OnPress onPress, Supplier<Integer> mode, boolean allowPulse) {
        return new ItemIconButton(
                x, y, ICON_SIZE, onPress,
                () -> redstoneIcon(mode.get(), allowPulse),
                () -> redstoneOverlay(mode.get(), allowPulse),
                Component.empty());
    }

    public static void renderTooltipLine(
            GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, Component line) {
        graphics.setTooltipForNextFrame(
                font,
                List.of(line.getVisualOrderText()),
                DefaultTooltipPositioner.INSTANCE,
                mouseX,
                mouseY,
                true);
    }
}
