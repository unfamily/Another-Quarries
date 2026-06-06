package net.unfamily.another_quarries.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.block.QuarryBlock;
import net.unfamily.another_quarries.block.structure.StructureQuarryBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AnotherQuarries.MOD_ID);

    private static final BlockBehaviour.Properties QUARRY_PROPERTIES =
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops();

    private static final BlockBehaviour.Properties STRUCTURE_PROPERTIES =
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.05f, 1.2f)
                    .sound(SoundType.COPPER)
                    .noOcclusion();

    public static final DeferredBlock<QuarryBlock> QUARRY =
            BLOCKS.register("quarry", () -> new QuarryBlock(QUARRY_PROPERTIES));

    public static final DeferredBlock<StructureQuarryBlock> STRUCTURE_QUARRY =
            BLOCKS.register("structure_quarry", () -> new StructureQuarryBlock(STRUCTURE_PROPERTIES));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private ModBlocks() {}
}
