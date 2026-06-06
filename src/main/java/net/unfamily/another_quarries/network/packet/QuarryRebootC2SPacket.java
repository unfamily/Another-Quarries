package net.unfamily.another_quarries.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;
import net.unfamily.another_quarries.client.gui.QuarryMenu;

public record QuarryRebootC2SPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<QuarryRebootC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "quarry_reboot"));

    public static final StreamCodec<FriendlyByteBuf, QuarryRebootC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, QuarryRebootC2SPacket::pos,
            QuarryRebootC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuarryRebootC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof QuarryBlockEntity quarry) {
                quarry.requestFullReboot();
                level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(),
                        SoundSource.BLOCKS, 0.3f, 0.9f);
                if (player.containerMenu instanceof QuarryMenu) {
                    player.containerMenu.broadcastChanges();
                }
            }
        });
    }
}
