package net.unfamily.another_quarries.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Valid-keys help overlay for the quarry destroy-filter GUI (subset of Another Dynamics filter help). */
public final class QuarryFilterHelpRenderer {
    public static final int HELP_TEXT_X = 14;
    private static final int GAP_BELOW_TITLE = 10;
    private static final int BODY_COLOR = QuarryFilterGuiLayout.FILTER_ENTRY_TEXT_COLOR;
    private static final int EXAMPLE_COLOR = 0xFF0066CC;
    private static final int EXAMPLE_HOVER_COLOR = 0xFF0066FF;

    private static final String PREFIX = "gui.another_quarries.quarry.filter.help.";

    private QuarryFilterHelpRenderer() {}

    public static final class ExampleHit {
        public final String example;
        public final int x;
        public final int y;
        public final int width;

        ExampleHit(String example, int x, int y, int width) {
            this.example = example;
            this.x = x;
            this.y = y;
            this.width = width;
        }
    }

    public static final class Frame {
        private final List<ExampleHit> examples = new ArrayList<>();

        public List<ExampleHit> examples() {
            return examples;
        }

        public void clearExamples() {
            examples.clear();
        }
    }

    public static void render(GuiGraphicsExtractor graphics, Font font, int panelMouseX, int panelMouseY, Frame frame) {
        frame.clearExamples();
        int titleBaseline = QuarryFilterGuiLayout.TITLE_Y;
        int lineStep = font.lineHeight + 4;
        int helpY = titleBaseline + font.lineHeight + GAP_BELOW_TITLE;
        helpY = renderLine(graphics, font, panelMouseX, panelMouseY, frame, PREFIX + "id", helpY);
        helpY += lineStep;
        helpY = renderLine(graphics, font, panelMouseX, panelMouseY, frame, PREFIX + "modid", helpY);
        helpY += lineStep;
        helpY = renderLine(graphics, font, panelMouseX, panelMouseY, frame, PREFIX + "tag", helpY);
        helpY += lineStep;
        renderLine(graphics, font, panelMouseX, panelMouseY, frame, PREFIX + "plain", helpY);
    }

    public static void renderExampleTooltip(
            GuiGraphicsExtractor graphics,
            Font font,
            Frame frame,
            int mouseX,
            int mouseY,
            int leftPos,
            int topPos) {
        ExampleHit hit = findHit(frame, mouseX, mouseY, leftPos, topPos, font.lineHeight);
        if (hit == null) {
            return;
        }
        List<FormattedCharSequence> lines = List.of(
                Component.translatable(PREFIX + "click_to_copy").getVisualOrderText(),
                Component.translatable(PREFIX + "paste_hint").getVisualOrderText());
        graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    public static boolean handleExampleClick(
            Frame frame,
            int mouseX,
            int mouseY,
            int leftPos,
            int topPos,
            int lineHeight,
            Consumer<String> onCopy) {
        ExampleHit hit = findHit(frame, mouseX, mouseY, leftPos, topPos, lineHeight);
        if (hit == null) {
            return false;
        }
        onCopy.accept(hit.example);
        return true;
    }

    private static ExampleHit findHit(Frame frame, int mouseX, int mouseY, int leftPos, int topPos, int lineHeight) {
        for (ExampleHit hit : frame.examples()) {
            int sx = leftPos + hit.x;
            int sy = topPos + hit.y;
            if (mouseX >= sx && mouseX <= sx + hit.width && mouseY >= sy && mouseY <= sy + lineHeight) {
                return hit;
            }
        }
        return null;
    }

    private static int renderLine(
            GuiGraphicsExtractor graphics,
            Font font,
            int panelMouseX,
            int panelMouseY,
            Frame frame,
            String keyPrefix,
            int y) {
        Component before = Component.translatable(keyPrefix);
        Component example = Component.translatable(keyPrefix + ".example");
        Component after = Component.translatable(keyPrefix + ".after");
        String beforeText = before.getString();
        String exampleText = example.getString();
        String afterText = after.getString();
        int x = HELP_TEXT_X;
        int beforeWidth = font.width(beforeText);
        graphics.text(font, before, x, y, BODY_COLOR, false);
        int exampleX = x + beforeWidth;
        int exampleWidth = font.width(exampleText);
        boolean hovered = panelMouseX >= exampleX && panelMouseX <= exampleX + exampleWidth
                && panelMouseY >= y && panelMouseY <= y + font.lineHeight;
        int exampleColor = hovered ? EXAMPLE_HOVER_COLOR : EXAMPLE_COLOR;
        graphics.text(font, Component.literal(exampleText), exampleX, y, exampleColor, false);
        if (hovered) {
            int underlineY = y + font.lineHeight;
            graphics.fill(exampleX, underlineY, exampleX + exampleWidth, underlineY + 1, exampleColor);
        }
        frame.examples().add(new ExampleHit(exampleText, exampleX, y, exampleWidth));
        if (!afterText.isEmpty()) {
            graphics.text(font, after, exampleX + exampleWidth, y, BODY_COLOR, false);
        }
        return y;
    }
}
