package net.unfamily.another_quarries.mining;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.unfamily.another_quarries.util.QuarryDiggingMode;

import java.util.List;

public record QuarryWorkContext(
        QuarryDrillType drill,
        int diggerModules,
        int speedModules,
        int fortuneLevel,
        boolean silkTouch,
        int rfPerBlock,
        Direction facing,
        QuarryDiggingMode diggingMode,
        ItemStack breakTool,
        boolean voidFilteredDrops,
        List<String> itemDenyFilters) {
}
