package net.unfamily.another_quarries.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.another_quarries.AnotherQuarries;

/** @deprecated Digging mode is now derived from quarry footprint size; packet is ignored. */
@Deprecated
public record QuarryDiggingModeC2SPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<QuarryDiggingModeC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "quarry_digging_mode"));

    public static final StreamCodec<FriendlyByteBuf, QuarryDiggingModeC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, QuarryDiggingModeC2SPacket::pos,
            QuarryDiggingModeC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuarryDiggingModeC2SPacket packet, IPayloadContext context) {
        // Digging mode is automatic from footprint size; manual toggle removed.
    }
}
