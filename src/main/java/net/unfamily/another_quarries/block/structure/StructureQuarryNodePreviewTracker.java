package net.unfamily.another_quarries.block.structure;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;

/** Last computed node-preview mask per position to avoid redundant block updates. */
final class StructureQuarryNodePreviewTracker {
    private static final int UNSET = Integer.MIN_VALUE;
    private static final Map<Long, Integer> LAST = new HashMap<>();

    private StructureQuarryNodePreviewTracker() {}

    static boolean noteChanged(BlockPos pos, int mask) {
        long key = pos.asLong();
        Integer previous = LAST.getOrDefault(key, UNSET);
        if (previous == mask) {
            return false;
        }
        if (mask == 0) {
            LAST.remove(key);
        } else {
            LAST.put(key, mask);
        }
        return true;
    }

    static void remove(BlockPos pos) {
        LAST.remove(pos.asLong());
    }
}
