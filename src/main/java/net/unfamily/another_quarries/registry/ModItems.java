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

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AnotherQuarries.MOD_ID);
    private static final Item.Properties ITEM_PROPERTIES = new Item.Properties();

    public static final DeferredItem<BlockItem> QUARRY = ITEMS.register("quarry",
            () -> new DescribedBlockItem(
                    ModBlocks.QUARRY.get(),
                    ITEM_PROPERTIES,
                    "tooltip.another_quarries.quarry.desc0",
                    "tooltip.another_quarries.quarry.desc1"));
    public static final DeferredItem<BlockItem> STRUCTURE_QUARRY =
            ITEMS.register("structure_quarry", () -> new BlockItem(ModBlocks.STRUCTURE_QUARRY.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> DRONE = ITEMS.register("drone",
            () -> new DescribedItem(ITEM_PROPERTIES, "tooltip.another_quarries.drone.desc"));
    public static final DeferredItem<Item> DRILL_DIAMOND = ITEMS.register("drill_diamond",
            () -> new DescribedItem(ITEM_PROPERTIES, "tooltip.another_quarries.drill_diamond.desc"));
    public static final DeferredItem<Item> DRILL_NETHERITE = ITEMS.register("drill_netherite",
            () -> new DescribedItem(ITEM_PROPERTIES, "tooltip.another_quarries.drill_netherite.desc"));
    public static final DeferredItem<Item> MODULE_BASE =
            ITEMS.register("module_base", () -> new Item(ITEM_PROPERTIES));
    public static final DeferredItem<Item> MODULE_SPEED = ITEMS.register("module_speed",
            () -> new DescribedItem(ITEM_PROPERTIES, "tooltip.another_quarries.module_speed.desc"));
    public static final DeferredItem<Item> MODULE_DIGGER = ITEMS.register("module_digger",
            () -> new DescribedItem(ITEM_PROPERTIES, "tooltip.another_quarries.module_digger.desc"));
    public static final DeferredItem<Item> MODULE_SILK_TOUCH = ITEMS.register("module_silktouch",
            () -> new DescribedItem(ITEM_PROPERTIES, "tooltip.another_quarries.module_silktouch.desc"));
    public static final DeferredItem<Item> MODULE_FORTUNE = ITEMS.register("module_fortune",
            () -> new DescribedItem(ITEM_PROPERTIES, "tooltip.another_quarries.module_fortune.desc"));
    public static final DeferredItem<Item> ARTIFICIAL_EYE =
            ITEMS.register("artificial_eye", () -> new Item(ITEM_PROPERTIES));

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
