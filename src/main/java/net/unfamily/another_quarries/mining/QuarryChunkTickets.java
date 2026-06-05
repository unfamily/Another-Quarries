package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Keeps mining target chunks loaded while the quarry chunk is active. Only current worker targets are forced.
 */
public final class QuarryChunkTickets {
    public static final TicketController CONTROLLER = new TicketController(
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "quarry_mining"),
            (level, helper) -> {
                for (BlockPos owner : helper.getBlockTickets().keySet()) {
                    if (!(level.getBlockEntity(owner) instanceof QuarryBlockEntity)) {
                        helper.removeAllTickets(owner);
                    }
                }
            });

    private QuarryChunkTickets() {}

    public static void sync(ServerLevel level, BlockPos owner, Iterable<BlockPos> activeTargets, LongOpenHashSet forcedChunks) {
        LongOpenHashSet needed = new LongOpenHashSet();
        for (BlockPos target : activeTargets) {
            needed.add(ChunkPos.pack(target));
        }

        LongOpenHashSet toRemove = new LongOpenHashSet(forcedChunks);
        toRemove.removeAll(needed);
        for (long chunk : toRemove) {
            ChunkPos chunkPos = ChunkPos.unpack(chunk);
            CONTROLLER.forceChunk(level, owner, chunkPos.x(), chunkPos.z(), false, false);
            forcedChunks.remove(chunk);
        }

        for (long chunk : needed) {
            if (forcedChunks.contains(chunk)) {
                continue;
            }
            ChunkPos chunkPos = ChunkPos.unpack(chunk);
            if (CONTROLLER.forceChunk(level, owner, chunkPos.x(), chunkPos.z(), true, false)) {
                forcedChunks.add(chunk);
            }
        }
    }

    public static void releaseAll(ServerLevel level, BlockPos owner, LongOpenHashSet forcedChunks) {
        for (long chunk : forcedChunks.toLongArray()) {
            ChunkPos chunkPos = ChunkPos.unpack(chunk);
            CONTROLLER.forceChunk(level, owner, chunkPos.x(), chunkPos.z(), false, false);
        }
        forcedChunks.clear();
    }
}
