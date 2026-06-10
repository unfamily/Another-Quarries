package net.unfamily.another_quarries.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.item.QuarryFilterModuleData;
import net.unfamily.another_quarries.network.packet.ClearPreviewForOwnerS2CPayload;
import net.unfamily.another_quarries.network.packet.PreviewMarkerS2CPayload;
import net.unfamily.another_quarries.network.packet.QuarryDiggingModeC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryFilterBulkSyncS2CPayload;
import net.unfamily.another_quarries.network.packet.QuarryFilterCopyToCopierC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryFilterLineUpdateC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryFilterPasteFromCopierC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryFilterRequestSyncC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryPreviewToggleC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryRebootC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarryRedstoneModeC2SPacket;
import net.unfamily.another_quarries.network.packet.QuarrySizeC2SPacket;

@EventBusSubscriber(modid = AnotherQuarries.MOD_ID)
public final class ModNetworking {
    private ModNetworking() {}

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(AnotherQuarries.MOD_ID).versioned("1");

        registrar.playToServer(QuarrySizeC2SPacket.TYPE, QuarrySizeC2SPacket.STREAM_CODEC, QuarrySizeC2SPacket::handle);
        registrar.playToServer(QuarryPreviewToggleC2SPacket.TYPE, QuarryPreviewToggleC2SPacket.STREAM_CODEC, QuarryPreviewToggleC2SPacket::handle);
        registrar.playToServer(QuarryDiggingModeC2SPacket.TYPE, QuarryDiggingModeC2SPacket.STREAM_CODEC, QuarryDiggingModeC2SPacket::handle);
        registrar.playToServer(QuarryRedstoneModeC2SPacket.TYPE, QuarryRedstoneModeC2SPacket.STREAM_CODEC, QuarryRedstoneModeC2SPacket::handle);
        registrar.playToServer(QuarryRebootC2SPacket.TYPE, QuarryRebootC2SPacket.STREAM_CODEC, QuarryRebootC2SPacket::handle);
        registrar.playToServer(QuarryFilterLineUpdateC2SPacket.TYPE, QuarryFilterLineUpdateC2SPacket.STREAM_CODEC, QuarryFilterLineUpdateC2SPacket::handle);
        registrar.playToServer(QuarryFilterRequestSyncC2SPacket.TYPE, QuarryFilterRequestSyncC2SPacket.STREAM_CODEC, QuarryFilterRequestSyncC2SPacket::handle);
        registrar.playToServer(QuarryFilterCopyToCopierC2SPacket.TYPE, QuarryFilterCopyToCopierC2SPacket.STREAM_CODEC, QuarryFilterCopyToCopierC2SPacket::handle);
        registrar.playToServer(QuarryFilterPasteFromCopierC2SPacket.TYPE, QuarryFilterPasteFromCopierC2SPacket.STREAM_CODEC, QuarryFilterPasteFromCopierC2SPacket::handle);

        registrar.playToClient(PreviewMarkerS2CPayload.TYPE, PreviewMarkerS2CPayload.STREAM_CODEC, PreviewMarkerS2CPayload::handle);
        registrar.playToClient(ClearPreviewForOwnerS2CPayload.TYPE, ClearPreviewForOwnerS2CPayload.STREAM_CODEC, ClearPreviewForOwnerS2CPayload::handle);
        registrar.playToClient(QuarryFilterBulkSyncS2CPayload.TYPE, QuarryFilterBulkSyncS2CPayload.STREAM_CODEC, QuarryFilterBulkSyncS2CPayload::handle);
    }

    public static void sendPreviewMarker(ServerPlayer player, BlockPos builderOrigin, BlockPos pos, int color, int durationTicks) {
        PacketDistributor.sendToPlayer(player, new PreviewMarkerS2CPayload(builderOrigin, pos, color, durationTicks));
    }

    public static void clearPreviewForBuilder(ServerPlayer player, BlockPos builderOrigin) {
        PacketDistributor.sendToPlayer(player, new ClearPreviewForOwnerS2CPayload(builderOrigin));
    }

    public static void sendFilterBulkSync(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PacketDistributor.sendToPlayer(
                player,
                new QuarryFilterBulkSyncS2CPayload(hand, QuarryFilterModuleData.getEditableDestroyList(stack)));
    }
}
