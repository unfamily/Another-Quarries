package net.unfamily.another_quarries.client.gui;

import net.minecraft.util.Mth;

/**
 * Filter-list scrollbar input (parity with Another Dynamics deny-filter view).
 */
public final class QuarryFilterScrollbarInput {
    private boolean draggingHandle;
    private int dragStartY;
    private int dragStartScrollOffset;

    public boolean isDraggingHandle() {
        return draggingHandle;
    }

    public void stopDragging() {
        draggingHandle = false;
    }

    public boolean handleScrollButtonClick(double mouseX, double mouseY, int leftPos, int topPos, int maxLines,
            int scrollOffset, ScrollOffsetConsumer setOffset) {
        if (maxLines <= QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES) {
            return false;
        }
        int gx = leftPos;
        int gy = topPos;
        if (mouseX >= gx + QuarryFilterGuiLayout.SCROLLBAR_X_REL
                && mouseX < gx + QuarryFilterGuiLayout.SCROLLBAR_X_REL + QuarryFilterGuiLayout.SCROLLBAR_WIDTH
                && mouseY >= gy + QuarryFilterGuiLayout.BUTTON_UP_Y_REL
                && mouseY < gy + QuarryFilterGuiLayout.BUTTON_UP_Y_REL + QuarryFilterGuiLayout.HANDLE_SIZE) {
            if (scrollUpSilent(maxLines, scrollOffset, setOffset)) {
                return true;
            }
        }
        if (mouseX >= gx + QuarryFilterGuiLayout.SCROLLBAR_X_REL
                && mouseX < gx + QuarryFilterGuiLayout.SCROLLBAR_X_REL + QuarryFilterGuiLayout.SCROLLBAR_WIDTH
                && mouseY >= gy + QuarryFilterGuiLayout.BUTTON_DOWN_Y_REL
                && mouseY < gy + QuarryFilterGuiLayout.BUTTON_DOWN_Y_REL + QuarryFilterGuiLayout.HANDLE_SIZE) {
            if (scrollDownSilent(maxLines, scrollOffset, setOffset)) {
                return true;
            }
        }
        return false;
    }

    public boolean handleHandleClick(double mouseX, double mouseY, int leftPos, int topPos, int maxLines,
            int scrollOffset) {
        int maxScroll = maxScroll(maxLines);
        if (maxLines <= QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES || maxScroll <= 0) {
            return false;
        }
        int gx = leftPos;
        int gy = topPos;
        double scrollRatio = (double) scrollOffset / maxScroll;
        int handleY = gy + QuarryFilterGuiLayout.SCROLLBAR_Y_REL
                + (int) (scrollRatio * (QuarryFilterGuiLayout.SCROLLBAR_HEIGHT - QuarryFilterGuiLayout.HANDLE_SIZE));
        if (mouseX >= gx + QuarryFilterGuiLayout.SCROLLBAR_X_REL
                && mouseX < gx + QuarryFilterGuiLayout.SCROLLBAR_X_REL + QuarryFilterGuiLayout.HANDLE_SIZE
                && mouseY >= handleY
                && mouseY < handleY + QuarryFilterGuiLayout.HANDLE_SIZE) {
            draggingHandle = true;
            dragStartY = (int) mouseY;
            dragStartScrollOffset = scrollOffset;
            return true;
        }
        return false;
    }

    public boolean handleTrackClick(double mouseX, double mouseY, int leftPos, int topPos, int maxLines,
            int scrollOffset, ScrollOffsetConsumer setOffset) {
        if (maxLines <= QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES) {
            return false;
        }
        int gx = leftPos;
        int gy = topPos;
        if (mouseX >= gx + QuarryFilterGuiLayout.SCROLLBAR_X_REL
                && mouseX < gx + QuarryFilterGuiLayout.SCROLLBAR_X_REL + QuarryFilterGuiLayout.SCROLLBAR_WIDTH
                && mouseY >= gy + QuarryFilterGuiLayout.SCROLLBAR_Y_REL
                && mouseY < gy + QuarryFilterGuiLayout.SCROLLBAR_Y_REL + QuarryFilterGuiLayout.SCROLLBAR_HEIGHT) {
            float clickRatio = (float) (mouseY - (gy + QuarryFilterGuiLayout.SCROLLBAR_Y_REL))
                    / QuarryFilterGuiLayout.SCROLLBAR_HEIGHT;
            clickRatio = Mth.clamp(clickRatio, 0.0f, 1.0f);
            int maxScroll = maxScroll(maxLines);
            int newOffset = (int) (clickRatio * maxScroll);
            if (newOffset != scrollOffset) {
                setOffset.set(newOffset);
            }
            return true;
        }
        return false;
    }

    public boolean handleDrag(double mouseY, int maxLines, ScrollOffsetConsumer setOffset) {
        if (!draggingHandle || maxLines <= QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES) {
            return false;
        }
        int maxScroll = maxScroll(maxLines);
        if (maxScroll <= 0) {
            return false;
        }
        int deltaY = (int) mouseY - dragStartY;
        float scrollRatio = (float) deltaY / (QuarryFilterGuiLayout.SCROLLBAR_HEIGHT - QuarryFilterGuiLayout.HANDLE_SIZE);
        int newOffset = dragStartScrollOffset + (int) (scrollRatio * maxScroll);
        setOffset.set(newOffset);
        return true;
    }

    public boolean handleWheel(double scrollY, int maxLines, int scrollOffset, ScrollOffsetConsumer setOffset) {
        if (maxLines <= QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES) {
            return false;
        }
        if (scrollY > 0) {
            return scrollUpSilent(maxLines, scrollOffset, setOffset);
        }
        if (scrollY < 0) {
            return scrollDownSilent(maxLines, scrollOffset, setOffset);
        }
        return false;
    }

    public static int maxScroll(int maxLines) {
        return Math.max(0, maxLines - QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES);
    }

    private static boolean scrollUpSilent(int maxLines, int scrollOffset, ScrollOffsetConsumer setOffset) {
        if (maxLines > QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES && scrollOffset > 0) {
            setOffset.set(scrollOffset - 1);
            return true;
        }
        return false;
    }

    private static boolean scrollDownSilent(int maxLines, int scrollOffset, ScrollOffsetConsumer setOffset) {
        int maxScroll = maxScroll(maxLines);
        if (maxLines > QuarryFilterGuiLayout.VISIBLE_FILTER_ENTRIES && scrollOffset < maxScroll) {
            setOffset.set(scrollOffset + 1);
            return true;
        }
        return false;
    }

    @FunctionalInterface
    public interface ScrollOffsetConsumer {
        void set(int offset);
    }
}
