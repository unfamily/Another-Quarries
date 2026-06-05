package net.unfamily.another_quarries.registry;

import java.util.function.UnaryOperator;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.block.QuarryBlock;
import net.unfamily.another_quarries.block.TrashCanBlock;
import net.unfamily.another_quarries.block.structure.StructureQuarryBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AnotherQuarries.MOD_ID);

    public static BlockBehaviour.Properties assignBlockId(Identifier key, UnaryOperator<BlockBehaviour.Properties> configure) {
        return configure.apply(BlockBehaviour.Properties.of())
                .setId(ResourceKey.create(Registries.BLOCK, key));
    }

    private static final UnaryOperator<BlockBehaviour.Properties> QUARRY_PROPERTIES =
            p -> p.mapColor(MapColor.STONE).strength(3.5f).sound(SoundType.STONE).requiresCorrectToolForDrops();

    private static final UnaryOperator<BlockBehaviour.Properties> STRUCTURE_PROPERTIES =
            p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.05f, 1.2f)
                    .sound(SoundType.COPPER)
                    .noOcclusion();

    private static final UnaryOperator<BlockBehaviour.Properties> TRASH_CAN_PROPERTIES =
            p -> p.mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops();

    public static final DeferredBlock<QuarryBlock> QUARRY =
            BLOCKS.register("quarry", key -> new QuarryBlock(assignBlockId(key, QUARRY_PROPERTIES)));

    public static final DeferredBlock<StructureQuarryBlock> STRUCTURE_QUARRY =
            BLOCKS.register("structure_quarry", key -> new StructureQuarryBlock(assignBlockId(key, STRUCTURE_PROPERTIES)));

    public static final DeferredBlock<TrashCanBlock> TRASH_CAN =
            BLOCKS.register("trash_can", key -> new TrashCanBlock(assignBlockId(key, TRASH_CAN_PROPERTIES)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private ModBlocks() {}
}
