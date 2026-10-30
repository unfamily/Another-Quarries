package net.unfamily.another_quarries.client;

import net.minecraft.core.BlockPos;

public final class ClientPreviewHandler {
    private ClientPreviewHandler() {}

    public static void handleShowAreaBorder(BlockPos owner, BlockPos corner1, BlockPos corner2, int color, int durationTicks) {
        QuarryFramePreviewRenderer.getInstance().showArea(owner, corner1, corner2, color, durationTicks);
    }

    public static void handleClearPreviewForOwner(BlockPos owner) {
        QuarryFramePreviewRenderer.getInstance().clearArea(owner);
    }
}
