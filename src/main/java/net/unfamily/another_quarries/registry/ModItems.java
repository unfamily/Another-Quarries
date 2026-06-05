package net.unfamily.another_quarries.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.another_quarries.AnotherQuarries;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AnotherQuarries.MOD_ID);

    public static final DeferredItem<BlockItem> QUARRY = ITEMS.registerSimpleBlockItem(ModBlocks.QUARRY);
    public static final DeferredItem<BlockItem> STRUCTURE_QUARRY = ITEMS.registerSimpleBlockItem(ModBlocks.STRUCTURE_QUARRY);
    public static final DeferredItem<BlockItem> TRASH_CAN = ITEMS.registerSimpleBlockItem(ModBlocks.TRASH_CAN);

    public static final DeferredItem<Item> DRONE = ITEMS.registerSimpleItem("drone");
    public static final DeferredItem<Item> DRILL_LASER = ITEMS.registerSimpleItem("drill_laser");
    public static final DeferredItem<Item> DRILL_DIAMOND = ITEMS.registerSimpleItem("drill_diamond");
    public static final DeferredItem<Item> DRILL_NETHERITE = ITEMS.registerSimpleItem("drill_netherite");
    public static final DeferredItem<Item> MODULE_BASE = ITEMS.registerSimpleItem("module_base");
    public static final DeferredItem<Item> MODULE_SPEED = ITEMS.registerSimpleItem("module_speed");
    public static final DeferredItem<Item> MODULE_DIGGER = ITEMS.registerSimpleItem("module_digger");
    public static final DeferredItem<Item> MODULE_SILK_TOUCH = ITEMS.registerSimpleItem("module_silktouch");
    public static final DeferredItem<Item> MODULE_FORTUNE = ITEMS.registerSimpleItem("module_fortune");
    public static final DeferredItem<Item> ARTIFICIAL_EYE = ITEMS.registerSimpleItem("artificial_eye");

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static boolean isDrone(ItemStack stack) {
        return stack.is(DRONE.get());
    }

    public static boolean isDrillLaser(ItemStack stack) {
        if (stack.is(DRILL_LASER.get())) {
            return true;
        }
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return AnotherQuarries.MOD_ID.equals(id.getNamespace()) && "drone_laser".equals(id.getPath());
    }

    public static boolean isDiamondDrill(ItemStack stack) {
        return stack.is(DRILL_DIAMOND.get());
    }

    public static boolean isNetheriteDrill(ItemStack stack) {
        return stack.is(DRILL_NETHERITE.get());
    }

    public static boolean isAnyDrill(ItemStack stack) {
        return isDiamondDrill(stack) || isNetheriteDrill(stack) || isDrillLaser(stack);
    }

    private ModItems() {}
}
