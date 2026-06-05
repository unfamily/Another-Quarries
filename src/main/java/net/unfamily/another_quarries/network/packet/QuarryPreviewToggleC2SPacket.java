package net.unfamily.another_quarries.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.network.ModNetworking;
import net.unfamily.another_quarries.util.QuarryPreview;

public record QuarryPreviewToggleC2SPacket(BlockPos pos, boolean enable) implements CustomPacketPayload {

    public static final Type<QuarryPreviewToggleC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "quarry_preview_toggle"));

    public static final StreamCodec<FriendlyByteBuf, QuarryPreviewToggleC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, QuarryPreviewToggleC2SPacket::pos,
            ByteBufCodecs.BOOL, QuarryPreviewToggleC2SPacket::enable,
            QuarryPreviewToggleC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuarryPreviewToggleC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (!(blockEntity instanceof QuarryBlockEntity quarry)) {
                return;
            }
            if (packet.enable()) {
                quarry.setPreviewEnabled(true);
                quarry.setChanged();
                QuarryPreview.sendFootprint(player, level, packet.pos(), quarry);
            } else {
                quarry.setPreviewEnabled(false);
                quarry.setChanged();
                ModNetworking.clearPreviewForBuilder(player, packet.pos());
            }
        });
    }
}
