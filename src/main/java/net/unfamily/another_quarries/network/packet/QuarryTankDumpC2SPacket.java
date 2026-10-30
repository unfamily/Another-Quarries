package net.unfamily.another_quarries.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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

/** C2S: discard all fluid in one internal quarry tank (GUI dump button). */
public record QuarryTankDumpC2SPacket(BlockPos pos, int tankIndex) implements CustomPacketPayload {

    public static final Type<QuarryTankDumpC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnotherQuarries.MOD_ID, "quarry_tank_dump"));

    public static final StreamCodec<FriendlyByteBuf, QuarryTankDumpC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, QuarryTankDumpC2SPacket::pos,
            ByteBufCodecs.VAR_INT, QuarryTankDumpC2SPacket::tankIndex,
            QuarryTankDumpC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuarryTankDumpC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof QuarryMenu menu) || !menu.getSyncedBlockPos().equals(packet.pos())) {
                return;
            }
            ServerLevel level = (ServerLevel) player.level();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof QuarryBlockEntity quarry && quarry.dumpTank(packet.tankIndex())) {
                level.playSound(null, packet.pos(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.25f, 1.0f);
                player.containerMenu.broadcastChanges();
            }
        });
    }
}
