package net.unfamily.another_quarries.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.unfamily.another_quarries.item.QuarryEquipmentInstaller;

import java.util.List;
import java.util.function.Consumer;

public class DescribedItem extends Item {
    private final List<String> tooltipKeys;

    @SafeVarargs
    public DescribedItem(Properties properties, String... tooltipKeys) {
        super(properties);
        this.tooltipKeys = List.of(tooltipKeys);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult install = QuarryEquipmentInstaller.useOnQuarryBlock(context);
        if (install != InteractionResult.PASS) {
            return install;
        }
        return super.useOn(context);
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
