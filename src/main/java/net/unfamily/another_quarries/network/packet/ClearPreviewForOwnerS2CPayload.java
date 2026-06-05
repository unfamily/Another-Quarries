package net.unfamily.another_quarries.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.client.ClientPreviewHandler;

public record ClearPreviewForOwnerS2CPayload(BlockPos owner) implements CustomPacketPayload {

    public static final Type<ClearPreviewForOwnerS2CPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "clear_preview_for_owner"));

    public static final StreamCodec<FriendlyByteBuf, ClearPreviewForOwnerS2CPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ClearPreviewForOwnerS2CPayload::owner,
            ClearPreviewForOwnerS2CPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearPreviewForOwnerS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPreviewHandler.handleClearPreviewForOwner(payload.owner()));
    }
}
