package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Force-loads chunks for active worker targets only while the quarry block is in a loaded chunk.
 * Never force-loads the quarry's own chunk.
 */
public final class QuarryChunkTickets {
    public static final TicketController CONTROLLER = new TicketController(
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "quarry_mining"),
            (level, helper) -> {
                for (BlockPos owner : helper.getBlockTickets().keySet()) {
                    if (!(level.getBlockEntity(owner) instanceof QuarryBlockEntity)) {
                        helper.removeAllTickets(owner);
                    }
                }
            });

    private QuarryChunkTickets() {}

    public static void sync(ServerLevel level, BlockPos owner, Iterable<BlockPos> activeTargets, LongOpenHashSet forcedChunks) {
        if (!level.isLoaded(owner)) {
            releaseAll(level, owner, forcedChunks);
            return;
        }

        long ownerChunk = ChunkPos.asLong(owner.getX() >> 4, owner.getZ() >> 4);
        LongOpenHashSet needed = new LongOpenHashSet();
        for (BlockPos target : activeTargets) {
            long targetChunk = ChunkPos.asLong(target.getX() >> 4, target.getZ() >> 4);
            if (targetChunk != ownerChunk) {
                needed.add(targetChunk);
            }
        }

        LongOpenHashSet toRemove = new LongOpenHashSet(forcedChunks);
        toRemove.removeAll(needed);
        for (long chunk : toRemove) {
            ChunkPos chunkPos = new ChunkPos(chunk);
            CONTROLLER.forceChunk(level, owner, chunkPos.x, chunkPos.z, false, false);
            forcedChunks.remove(chunk);
        }

        for (long chunk : needed) {
            if (forcedChunks.contains(chunk)) {
                continue;
            }
            ChunkPos chunkPos = new ChunkPos(chunk);
            if (CONTROLLER.forceChunk(level, owner, chunkPos.x, chunkPos.z, true, false)) {
                forcedChunks.add(chunk);
            }
        }
    }

    public static void releaseAll(ServerLevel level, BlockPos owner, LongOpenHashSet forcedChunks) {
        for (long chunk : forcedChunks.toLongArray()) {
            ChunkPos chunkPos = new ChunkPos(chunk);
            CONTROLLER.forceChunk(level, owner, chunkPos.x, chunkPos.z, false, false);
        }
        forcedChunks.clear();
    }
}
