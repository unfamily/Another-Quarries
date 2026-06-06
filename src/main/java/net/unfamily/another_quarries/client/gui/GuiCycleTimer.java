package net.unfamily.another_quarries.client.gui;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class GuiCycleTimer {
    private final Supplier<Integer> cycleTimeMs;
    private long startTime;
    private long drawTime;
    private long pausedDuration;

    public GuiCycleTimer(Supplier<Integer> cycleTimeMs) {
        this.cycleTimeMs = cycleTimeMs;
        long time = System.currentTimeMillis();
        this.startTime = time - cycleTimeMs.get();
        this.drawTime = time;
    }

    public @Nullable <T> T get(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        int ms = Math.max(1, cycleTimeMs.get());
        long index = ((drawTime - startTime) / ms) % list.size();
        return list.get(Math.toIntExact(index));
    }

    public <T> T getOrDefault(List<T> list, T defaultValue) {
        return Optional.ofNullable(get(list)).orElse(defaultValue);
    }

    public void onDraw() {
        if (!Screen.hasShiftDown()) {
            if (pausedDuration > 0) {
                startTime += pausedDuration;
                pausedDuration = 0;
            }
            drawTime = System.currentTimeMillis();
        } else {
            pausedDuration = System.currentTimeMillis() - drawTime;
        }
    }
}
