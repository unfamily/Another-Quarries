package net.unfamily.another_quarries.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.network.InteractionHandNetworking;
import net.unfamily.another_quarries.client.gui.QuarryFilterModuleMenu;
import net.unfamily.another_quarries.item.QuarryFilterModuleData;
import net.unfamily.another_quarries.network.ModNetworking;

public record QuarryFilterLineUpdateC2SPacket(InteractionHand hand, int index, String text)
        implements CustomPacketPayload {

    public static final Type<QuarryFilterLineUpdateC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "quarry_filter_line_update"));

    public static final StreamCodec<FriendlyByteBuf, QuarryFilterLineUpdateC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    InteractionHandNetworking.STREAM_CODEC, QuarryFilterLineUpdateC2SPacket::hand,
                    ByteBufCodecs.INT, QuarryFilterLineUpdateC2SPacket::index,
                    ByteBufCodecs.STRING_UTF8, QuarryFilterLineUpdateC2SPacket::text,
                    QuarryFilterLineUpdateC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuarryFilterLineUpdateC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof QuarryFilterModuleMenu menu)
                    || menu.getEditHand() != packet.hand()) {
                return;
            }
            if (!QuarryFilterModuleMenu.isValidHandStack(player, packet.hand())) {
                return;
            }
            menu.setServerDraftLine(packet.index(), packet.text());
            ModNetworking.sendFilterBulkSync(player, packet.hand(), menu.getServerDraftForSync());
        });
    }
}
