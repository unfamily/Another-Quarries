package net.unfamily.another_quarries.client;

import net.minecraft.core.BlockPos;
import net.unfamily.iskalib.client.marker.MarkRenderer;

public final class ClientPreviewHandler {
    private ClientPreviewHandler() {}

    public static void handleAddOwnedBillboard(BlockPos owner, BlockPos pos, int color, int durationTicks) {
        MarkRenderer.getInstance().addBillboardMarker(owner, pos, color, durationTicks);
    }

    public static void handleClearPreviewForOwner(BlockPos owner) {
        MarkRenderer.getInstance().clearBillboardMarkersForOwner(owner);
    }
}
