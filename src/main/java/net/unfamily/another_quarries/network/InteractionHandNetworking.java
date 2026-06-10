package net.unfamily.another_quarries.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;

public final class InteractionHandNetworking {
    public static final StreamCodec<FriendlyByteBuf, InteractionHand> STREAM_CODEC = StreamCodec.of(
            (buf, hand) -> buf.writeByte(hand == InteractionHand.MAIN_HAND ? 0 : 1),
            buf -> buf.readByte() == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);

    private InteractionHandNetworking() {}

    public static void encode(FriendlyByteBuf buf, InteractionHand hand) {
        STREAM_CODEC.encode(buf, hand);
    }

    public static InteractionHand decode(FriendlyByteBuf buf) {
        return STREAM_CODEC.decode(buf);
    }
}
