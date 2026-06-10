package net.unfamily.another_quarries.integration.jei.ghost;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

public class QuarryGhostIngredientHandler<T extends Screen> implements IGhostIngredientHandler<T> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(T gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        if (gui instanceof IQuarryGhostTarget ghostTarget) {
            tryAddTarget(targets, ghostTarget, ingredient.getIngredient());
        }
        return targets;
    }

    @SuppressWarnings("unchecked")
    private <I> void tryAddTarget(List<Target<I>> targets, IQuarryGhostTarget ghostTarget, I ingredient) {
        IQuarryGhostTarget.IGhostIngredientConsumer consumer = ghostTarget.getGhostHandler();
        if (consumer == null) {
            return;
        }
        Object validated = consumer.supportedTarget(ingredient);
        if (validated == null) {
            return;
        }
        Rect2i area = ghostTarget.getGhostTargetArea();
        if (area == null) {
            return;
        }
        targets.add(new Target<>() {
            @Override
            public Rect2i getArea() {
                return area;
            }

            @Override
            public void accept(I ingredientDropped) {
                consumer.accept(validated);
            }
        });
    }

    @Override
    public void onComplete() {}

    @Override
    public boolean shouldHighlightTargets() {
        return true;
    }
}
