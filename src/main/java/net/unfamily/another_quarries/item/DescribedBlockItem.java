package net.unfamily.another_quarries.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;

public class DescribedBlockItem extends BlockItem {
    private final List<String> tooltipKeys;

    @SafeVarargs
    public DescribedBlockItem(Block block, Properties properties, String... tooltipKeys) {
        super(block, properties);
        this.tooltipKeys = List.of(tooltipKeys);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltipAdder,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, tooltipAdder, tooltipFlag);
        for (String key : tooltipKeys) {
            tooltipAdder.accept(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }
}
