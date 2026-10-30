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
    /** Same as IskaLib {@code AreaBorderRenderer.DEFAULT_MACHINE_COLOR}. */
    public static final int PREVIEW_COLOR = 0x80FF00FF;

    private QuarryPreview() {}

    public static void sendFootprint(ServerPlayer player, ServerLevel level, BlockPos quarryPos, QuarryBlockEntity quarry) {
        var state = level.getBlockState(quarryPos);
        if (!(state.getBlock() instanceof QuarryBlock)) {
            return;
        }
        var facing = state.getValue(HorizontalDirectionalBlock.FACING);
        // Outer shell AABB matches the structure frame footprint (pillars + floor/ceiling rim).
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
        // AABB max is exclusive (+1); frame preview expects inclusive block corners
        int maxX = (int) Math.floor(aabb.maxX) - 1;
        int maxY = (int) Math.floor(aabb.maxY) - 1;
        int maxZ = (int) Math.floor(aabb.maxZ) - 1;

        ModNetworking.sendAreaBorderPreview(
                player,
                quarryPos,
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ),
                PREVIEW_COLOR,
                0);
    }
}
