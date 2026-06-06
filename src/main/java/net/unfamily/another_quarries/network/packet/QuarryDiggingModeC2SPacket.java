package net.unfamily.another_quarries.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;

public record QuarryDiggingModeC2SPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<QuarryDiggingModeC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "quarry_digging_mode"));

    public static final StreamCodec<FriendlyByteBuf, QuarryDiggingModeC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, QuarryDiggingModeC2SPacket::pos,
            QuarryDiggingModeC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuarryDiggingModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (!(blockEntity instanceof QuarryBlockEntity quarry)) {
                return;
            }
            if (!quarry.requiresChunkDiggingMode()) {
                quarry.toggleDiggingMode();
            }
        });
    }
}
