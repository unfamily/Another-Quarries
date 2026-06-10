package net.unfamily.another_quarries.client.gui;

import net.minecraft.world.InteractionHand;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side queue for filter bulk sync when the filter module menu is not open yet. */
public final class QuarryFilterModuleClientSync {
    private static final Map<InteractionHand, List<String>> PENDING = new ConcurrentHashMap<>();

    private QuarryFilterModuleClientSync() {}

    public static void queueOrApply(QuarryFilterModuleMenu menu, InteractionHand hand, List<String> lines) {
        if (menu != null && menu.getEditHand() == hand) {
            menu.applyClientDestroyFilters(lines);
            PENDING.remove(hand);
            return;
        }
        PENDING.put(hand, lines);
    }

    public static void consumePending(QuarryFilterModuleMenu menu) {
        if (menu == null) {
            return;
        }
        List<String> pending = PENDING.remove(menu.getEditHand());
        if (pending != null) {
            menu.applyClientDestroyFilters(pending);
        }
    }
}
