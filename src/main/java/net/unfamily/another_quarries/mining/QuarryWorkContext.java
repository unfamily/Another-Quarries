package net.unfamily.another_quarries.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.unfamily.another_quarries.util.QuarryDiggingMode;

public record QuarryWorkContext(
        QuarryDrillType drill,
        int diggerModules,
        int speedModules,
        int fortuneLevel,
        boolean silkTouch,
        int rfPerBlock,
        Direction facing,
        QuarryDiggingMode diggingMode) {
}
