package net.unfamily.another_quarries.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DescribedItem extends Item {
    private final List<String> tooltipKeys;

    @SafeVarargs
    public DescribedItem(Properties properties, String... tooltipKeys) {
        super(properties);
        this.tooltipKeys = List.of(tooltipKeys);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        for (String key : tooltipKeys) {
            tooltipComponents.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }
}
