package net.unfamily.another_quarries.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.unfamily.another_quarries.block.entity.TrashCanBlockEntity;
import net.unfamily.another_quarries.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class TrashCanBlock extends BaseEntityBlock {
    public static final MapCodec<TrashCanBlock> CODEC = simpleCodec(TrashCanBlock::new);

    public TrashCanBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrashCanBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.TRASH_CAN_BE.get(), TrashCanBlock::serverTick);
    }

    private static void serverTick(Level level, BlockPos pos, BlockState state, TrashCanBlockEntity blockEntity) {
        AABB pickupBox = new AABB(pos).inflate(0.25, 0.5, 0.25);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, pickupBox);
        for (ItemEntity item : items) {
            item.discard();
        }
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        player.setItemInHand(hand, ItemStack.EMPTY);
        return InteractionResult.CONSUME;
    }
}
