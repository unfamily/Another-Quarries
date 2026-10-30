package net.unfamily.another_quarries.client;

import net.minecraft.core.BlockPos;
import net.unfamily.iskalib.client.marker.AreaBorderRenderer;

public final class ClientPreviewHandler {
    private ClientPreviewHandler() {}

    public static void handleShowAreaBorder(BlockPos owner, BlockPos corner1, BlockPos corner2, int color, int durationTicks) {
        AreaBorderRenderer.getInstance().showArea(owner, corner1, corner2, color, durationTicks);
    }

    public static void handleClearPreviewForOwner(BlockPos owner) {
        AreaBorderRenderer.getInstance().clearArea(owner);
    }
}
