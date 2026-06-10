package net.unfamily.another_quarries.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.item.DescribedBlockItem;
import net.unfamily.another_quarries.item.DescribedItem;
import net.unfamily.another_quarries.item.QuarryModules;

import java.util.function.UnaryOperator;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AnotherQuarries.MOD_ID);

    public static final DeferredItem<BlockItem> QUARRY = ITEMS.registerItem("quarry",
            props -> new DescribedBlockItem(
                    ModBlocks.QUARRY.get(),
                    props,
                    "tooltip.another_quarries.quarry.desc0",
                    "tooltip.another_quarries.quarry.desc1"),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<BlockItem> STRUCTURE_QUARRY =
            ITEMS.registerSimpleBlockItem(ModBlocks.STRUCTURE_QUARRY);

    public static final DeferredItem<Item> DRONE = ITEMS.registerItem("drone",
            props -> new DescribedItem(props, "tooltip.another_quarries.drone.desc"),
            UnaryOperator.identity());
    public static final DeferredItem<Item> DRILL_DIAMOND = ITEMS.registerItem("drill_diamond",
            props -> new DescribedItem(props, "tooltip.another_quarries.drill_diamond.desc"),
            UnaryOperator.identity());
    public static final DeferredItem<Item> DRILL_NETHERITE = ITEMS.registerItem("drill_netherite",
            props -> new DescribedItem(props, "tooltip.another_quarries.drill_netherite.desc"),
            UnaryOperator.identity());
    public static final DeferredItem<Item> MODULE_BASE = ITEMS.registerSimpleItem("module_base");
    public static final DeferredItem<Item> MODULE_SPEED = ITEMS.registerItem("module_speed",
            props -> new DescribedItem(props, "tooltip.another_quarries.module_speed.desc"),
            UnaryOperator.identity());
    public static final DeferredItem<Item> MODULE_DIGGER = ITEMS.registerItem("module_digger",
            props -> new DescribedItem(props, "tooltip.another_quarries.module_digger.desc"),
            UnaryOperator.identity());
    public static final DeferredItem<Item> MODULE_SILK_TOUCH = ITEMS.registerItem("module_silktouch",
            props -> new DescribedItem(props, "tooltip.another_quarries.module_silktouch.desc"),
            UnaryOperator.identity());
    public static final DeferredItem<Item> MODULE_FORTUNE = ITEMS.registerItem("module_fortune",
            props -> new DescribedItem(props, "tooltip.another_quarries.module_fortune.desc"),
            UnaryOperator.identity());
    public static final DeferredItem<Item> ARTIFICIAL_EYE = ITEMS.registerSimpleItem("artificial_eye");

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static boolean isDrone(ItemStack stack) {
        return stack.is(DRONE.get());
    }

    public static boolean isDiamondDrill(ItemStack stack) {
        return stack.is(DRILL_DIAMOND.get());
    }

    public static boolean isNetheriteDrill(ItemStack stack) {
        return stack.is(DRILL_NETHERITE.get());
    }

    public static boolean isAnyDrill(ItemStack stack) {
        return isDiamondDrill(stack) || isNetheriteDrill(stack);
    }

    public static boolean isQuarryEquipment(ItemStack stack) {
        return isDrone(stack) || isAnyDrill(stack) || QuarryModules.isModule(stack);
    }

    /** Maps removed laser drill items to netherite for existing saves. */
    public static ItemStack migrateRemovedDrill(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (AnotherQuarries.MOD_ID.equals(id.getNamespace())
                && ("drill_laser".equals(id.getPath()) || "drone_laser".equals(id.getPath()))) {
            return new ItemStack(DRILL_NETHERITE.get(), stack.getCount());
        }
        return stack;
    }

    private ModItems() {}
}
