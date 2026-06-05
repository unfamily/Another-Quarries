package net.unfamily.another_quarries.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.client.gui.QuarryMenu;
import net.unfamily.another_quarries.network.ModNetworking;
import net.unfamily.another_quarries.util.QuarryPreview;

public record QuarrySizeC2SPacket(BlockPos pos, int direction, boolean increment, int amount)
        implements CustomPacketPayload {

    public static final Type<QuarrySizeC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "quarry_size"));

    public static final StreamCodec<FriendlyByteBuf, QuarrySizeC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, QuarrySizeC2SPacket::pos,
            ByteBufCodecs.INT, QuarrySizeC2SPacket::direction,
            ByteBufCodecs.BOOL, QuarrySizeC2SPacket::increment,
            ByteBufCodecs.INT, QuarrySizeC2SPacket::amount,
            QuarrySizeC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuarrySizeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ServerLevel level = (ServerLevel) player.level();
            QuarryBlockEntity quarry = resolveQuarry(player, level, packet.pos());
            if (quarry == null) {
                return;
            }
            int amount = Math.max(1, Math.min(10, packet.amount()));
            quarry.adjustSize(packet.direction(), packet.increment(), amount);
            level.playSound(null, quarry.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.BLOCKS, 0.3f, 1.0f);
            if (player.containerMenu instanceof QuarryMenu) {
                player.containerMenu.broadcastChanges();
            }
            if (quarry.isPreviewEnabled()) {
                ModNetworking.clearPreviewForBuilder(player, quarry.getBlockPos());
                QuarryPreview.sendFootprint(player, level, quarry.getBlockPos(), quarry);
            }
        });
    }

    private static QuarryBlockEntity resolveQuarry(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (player.containerMenu instanceof QuarryMenu menu) {
            QuarryBlockEntity menuQuarry = menu.getBlockEntity();
            if (menuQuarry != null && menuQuarry.getBlockPos().equals(pos)) {
                return menuQuarry;
            }
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof QuarryBlockEntity quarry) {
            return quarry;
        }
        return null;
    }
}
