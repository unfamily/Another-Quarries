package net.unfamily.another_quarries.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.unfamily.another_quarries.block.QuarryBlock;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.network.ModNetworking;

public final class QuarryPreview {
    private QuarryPreview() {}

    public static void sendFootprint(ServerPlayer player, ServerLevel level, BlockPos quarryPos, QuarryBlockEntity quarry) {
        var state = level.getBlockState(quarryPos);
        if (!(state.getBlock() instanceof QuarryBlock)) {
            return;
        }
        var facing = state.getValue(HorizontalDirectionalBlock.FACING);
        AABB aabb = QuarryAreaLogic.getMiningVolumeAABB(
                quarryPos,
                facing,
                quarry.getSizeLeft(),
                quarry.getSizeRight(),
                quarry.getSizeHeight(),
                quarry.getSizeDepth());

        int minX = (int) Math.floor(aabb.minX);
        int minY = (int) Math.floor(aabb.minY);
        int minZ = (int) Math.floor(aabb.minZ);
        int maxX = (int) Math.floor(aabb.maxX);
        int maxY = (int) Math.floor(aabb.maxY);
        int maxZ = (int) Math.floor(aabb.maxZ);

        int color = 0x80FF00FF;
        int durationTicks = 0;

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                if (x == minX || x == maxX - 1 || z == minZ || z == maxZ - 1) {
                    ModNetworking.sendPreviewMarker(player, quarryPos, new BlockPos(x, maxY - 1, z), color, durationTicks);
                    ModNetworking.sendPreviewMarker(player, quarryPos, new BlockPos(x, minY, z), color, durationTicks);
                }
            }
        }
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                if (x == minX || x == maxX - 1 || y == minY || y == maxY - 1) {
                    ModNetworking.sendPreviewMarker(player, quarryPos, new BlockPos(x, y, minZ), color, durationTicks);
                    ModNetworking.sendPreviewMarker(player, quarryPos, new BlockPos(x, y, maxZ - 1), color, durationTicks);
                }
            }
        }
        for (int z = minZ; z < maxZ; z++) {
            for (int y = minY; y < maxY; y++) {
                if (z == minZ || z == maxZ - 1 || y == minY || y == maxY - 1) {
                    ModNetworking.sendPreviewMarker(player, quarryPos, new BlockPos(minX, y, z), color, durationTicks);
                    ModNetworking.sendPreviewMarker(player, quarryPos, new BlockPos(maxX - 1, y, z), color, durationTicks);
                }
            }
        }
    }
}
