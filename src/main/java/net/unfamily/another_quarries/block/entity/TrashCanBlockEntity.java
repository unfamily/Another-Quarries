package net.unfamily.another_quarries.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.another_quarries.registry.ModBlockEntities;
import net.unfamily.another_quarries.transfer.VoidItemResourceHandler;

public final class TrashCanBlockEntity extends BlockEntity {
    private final ResourceHandler<ItemResource> itemHandler = VoidItemResourceHandler.INSTANCE;

    public TrashCanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRASH_CAN_BE.get(), pos, state);
    }

    public ResourceHandler<ItemResource> getItemHandler() {
        return itemHandler;
    }
}
