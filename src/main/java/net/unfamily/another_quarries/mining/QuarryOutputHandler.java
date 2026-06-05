package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;

public final class QuarryOutputHandler {
    private QuarryOutputHandler() {}

    public static boolean hasBufferSpace(ItemStackHandler buffer) {
        for (int i = 0; i < buffer.getSlots(); i++) {
            ItemStack stack = buffer.getStackInSlot(i);
            if (stack.isEmpty()) {
                return true;
            }
            if (stack.getCount() < buffer.getSlotLimit(i)) {
                return true;
            }
        }
        return false;
    }

    public static void tryEjectBufferUp(ServerLevel level, BlockPos quarryPos, ItemStackHandler buffer) {
        BlockPos above = quarryPos.above();
        BlockState state = level.getBlockState(above);
        var blockEntity = level.getBlockEntity(above);
        ResourceHandler<ItemResource> destination = level.getCapability(
                Capabilities.Item.BLOCK, above, state, blockEntity, Direction.DOWN);
        if (destination == null) {
            return;
        }
        ResourceHandler<ItemResource> source = LegacyItemHandlerResourceHandler.wrap(buffer);
        if (!canAcceptAny(source, destination)) {
            return;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int moved = ResourceHandlerUtil.moveStacking(
                    source, destination, resource -> !resource.isEmpty(), Integer.MAX_VALUE, tx);
            if (moved > 0) {
                tx.commit();
            }
        }
    }

    private static boolean canAcceptAny(ResourceHandler<ItemResource> source, ResourceHandler<ItemResource> destination) {
        try (Transaction tx = Transaction.openRoot()) {
            int moved = ResourceHandlerUtil.moveStacking(
                    source, destination, resource -> !resource.isEmpty(), Integer.MAX_VALUE, tx);
            return moved > 0;
        }
    }
}
