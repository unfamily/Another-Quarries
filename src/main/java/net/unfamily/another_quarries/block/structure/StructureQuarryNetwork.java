package net.unfamily.another_quarries.block.structure;

import net.minecraft.world.level.block.Block;
import net.unfamily.another_quarries.registry.ModBlocks;

public final class StructureQuarryNetwork {
    private StructureQuarryNetwork() {}

    public static boolean isStructureQuarry(Block block) {
        return block == ModBlocks.STRUCTURE_QUARRY.get();
    }
}
