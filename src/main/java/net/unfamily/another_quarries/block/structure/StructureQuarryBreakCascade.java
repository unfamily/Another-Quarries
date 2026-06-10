package net.unfamily.another_quarries.block.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.config.ModConfig;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Slow connected break for player-mined {@code structure_quarry} blocks. */
public final class StructureQuarryBreakCascade {
    private static final ThreadLocal<Integer> SUPPRESS_DEPTH = ThreadLocal.withInitial(() -> 0);

    private static final Map<ResourceKey<Level>, ArrayDeque<BlockPos>> QUEUES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> QUEUED = new HashMap<>();

    private StructureQuarryBreakCascade() {}

    public static void runWithoutCascade(Runnable action) {
        SUPPRESS_DEPTH.set(SUPPRESS_DEPTH.get() + 1);
        try {
            action.run();
        } finally {
            int depth = SUPPRESS_DEPTH.get() - 1;
            if (depth <= 0) {
                SUPPRESS_DEPTH.remove();
            } else {
                SUPPRESS_DEPTH.set(depth);
            }
        }
    }

    public static boolean isCascadeSuppressed() {
        return SUPPRESS_DEPTH.get() > 0;
    }

    public static void startFromPlayerBreak(ServerLevel level, BlockPos origin) {
        if (!ModConfig.structureQuarryCascadeBreakEnabled() || isCascadeSuppressed()) {
            return;
        }

        Set<BlockPos> component = floodFillStructure(level, origin);
        component.remove(origin.immutable());

        int maxBlocks = ModConfig.structureQuarryCascadeMaxBlocks();
        if (component.size() > maxBlocks) {
            AnotherQuarries.LOGGER.debug(
                    "Structure quarry cascade capped at {} blocks (component size {})",
                    maxBlocks,
                    component.size());
        }

        ResourceKey<Level> dimension = level.dimension();
        ArrayDeque<BlockPos> queue = QUEUES.computeIfAbsent(dimension, key -> new ArrayDeque<>());
        Set<BlockPos> queued = QUEUED.computeIfAbsent(dimension, key -> new HashSet<>());

        int added = 0;
        for (BlockPos pos : component) {
            if (added >= maxBlocks) {
                break;
            }
            if (queued.add(pos.immutable())) {
                queue.addLast(pos.immutable());
                added++;
            }
        }
    }

    public static void tick(ServerLevel level) {
        if ((level.getGameTime() & 1L) != 0L) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        ArrayDeque<BlockPos> queue = QUEUES.get(dimension);
        if (queue == null || queue.isEmpty()) {
            return;
        }

        Set<BlockPos> queued = QUEUED.get(dimension);
        final int blockBudget = ModConfig.structureQuarryCascadeBlocksPerTick();

        runWithoutCascade(() -> {
            int processed = 0;
            while (processed < blockBudget && !queue.isEmpty()) {
                processed++;
                BlockPos pos = queue.pollFirst();
                if (queued != null) {
                    queued.remove(pos);
                }
                BlockState state = level.getBlockState(pos);
                if (!StructureQuarryNetwork.isStructureQuarry(state.getBlock())) {
                    continue;
                }
                level.destroyBlock(pos, true);
            }
        });

        if (queue.isEmpty()) {
            QUEUES.remove(dimension);
            if (queued != null) {
                QUEUED.remove(dimension);
            }
        }
    }

    private static Set<BlockPos> floodFillStructure(ServerLevel level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        pending.addLast(start.immutable());

        while (!pending.isEmpty()) {
            BlockPos pos = pending.pollFirst();
            if (!visited.add(pos)) {
                continue;
            }
            if (!StructureQuarryNetwork.isStructureQuarry(level.getBlockState(pos).getBlock())) {
                visited.remove(pos);
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.contains(next)) {
                    pending.addLast(next);
                }
            }
        }
        return visited;
    }
}
