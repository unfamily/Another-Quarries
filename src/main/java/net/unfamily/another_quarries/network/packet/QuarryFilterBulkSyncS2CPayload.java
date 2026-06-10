package net.unfamily.another_quarries.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.client.gui.QuarryFilterModuleClientSync;
import net.unfamily.another_quarries.client.gui.QuarryFilterModuleMenu;

import java.util.ArrayList;
import java.util.List;

public record QuarryFilterBulkSyncS2CPayload(InteractionHand hand, List<String> lines)
        implements CustomPacketPayload {

    public static final Type<QuarryFilterBulkSyncS2CPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "quarry_filter_bulk_sync"));

    public static final StreamCodec<FriendlyByteBuf, QuarryFilterBulkSyncS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    InteractionHand.STREAM_CODEC, QuarryFilterBulkSyncS2CPayload::hand,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
                    QuarryFilterBulkSyncS2CPayload::lines,
                    QuarryFilterBulkSyncS2CPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuarryFilterBulkSyncS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            QuarryFilterModuleMenu menu = context.player().containerMenu instanceof QuarryFilterModuleMenu filterMenu
                    ? filterMenu
                    : null;
            QuarryFilterModuleClientSync.queueOrApply(menu, payload.hand(), payload.lines());
        });
    }
}
