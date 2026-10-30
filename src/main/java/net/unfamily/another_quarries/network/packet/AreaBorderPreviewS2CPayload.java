package net.unfamily.another_quarries.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.client.ClientPreviewHandler;

public record AreaBorderPreviewS2CPayload(BlockPos owner, BlockPos corner1, BlockPos corner2, int color, int durationTicks)
        implements CustomPacketPayload {

    public static final Type<AreaBorderPreviewS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "area_border_preview"));

    public static final StreamCodec<FriendlyByteBuf, AreaBorderPreviewS2CPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AreaBorderPreviewS2CPayload::owner,
            BlockPos.STREAM_CODEC, AreaBorderPreviewS2CPayload::corner1,
            BlockPos.STREAM_CODEC, AreaBorderPreviewS2CPayload::corner2,
            ByteBufCodecs.INT, AreaBorderPreviewS2CPayload::color,
            ByteBufCodecs.INT, AreaBorderPreviewS2CPayload::durationTicks,
            AreaBorderPreviewS2CPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AreaBorderPreviewS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPreviewHandler.handleShowAreaBorder(
                payload.owner(), payload.corner1(), payload.corner2(), payload.color(), payload.durationTicks()));
    }
}
