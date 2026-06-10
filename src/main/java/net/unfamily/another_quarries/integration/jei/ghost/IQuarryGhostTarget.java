package net.unfamily.another_quarries.integration.jei.ghost;

import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

/** Screen contract for JEI ghost-ingredient drops into the filter calibration slot. */
public interface IQuarryGhostTarget {
    @Nullable
    IGhostIngredientConsumer getGhostHandler();

    @Nullable
    default Rect2i getGhostTargetArea() {
        return null;
    }

    interface IGhostIngredientConsumer {
        @Nullable
        Object supportedTarget(Object ingredient);

        void accept(Object ingredient);
    }
}
