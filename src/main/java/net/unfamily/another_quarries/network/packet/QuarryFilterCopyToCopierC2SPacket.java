package net.unfamily.another_quarries.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.client.gui.QuarryFilterModuleMenu;
import net.unfamily.another_quarries.integration.anotherdynamics.QuarryFilterCopierCompat;

public record QuarryFilterCopyToCopierC2SPacket(InteractionHand hand) implements CustomPacketPayload {

    public static final Type<QuarryFilterCopyToCopierC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "quarry_filter_copy_copier"));

    public static final StreamCodec<FriendlyByteBuf, QuarryFilterCopyToCopierC2SPacket> STREAM_CODEC =
            StreamCodec.composite(InteractionHand.STREAM_CODEC, QuarryFilterCopyToCopierC2SPacket::hand,
                    QuarryFilterCopyToCopierC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuarryFilterCopyToCopierC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof QuarryFilterModuleMenu menu)
                    || menu.getEditHand() != packet.hand()) {
                return;
            }
            QuarryFilterCopierCompat.copyToCopier(player, packet.hand()).sendTo(player);
        });
    }
}
