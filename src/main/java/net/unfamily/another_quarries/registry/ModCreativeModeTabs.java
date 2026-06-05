package net.unfamily.another_quarries.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.another_quarries.AnotherQuarries;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnotherQuarries.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANOTHER_QUARRIES_TAB =
            CREATIVE_MODE_TABS.register("another_quarries", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.another_quarries.tab"))
                    .icon(() -> new ItemStack(ModItems.QUARRY.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.QUARRY.get());
                        output.accept(ModItems.DRONE.get());
                        output.accept(ModItems.ARTIFICIAL_EYE.get());
                        output.accept(ModItems.DRILL_DIAMOND.get());
                        output.accept(ModItems.DRILL_NETHERITE.get());
                        output.accept(ModItems.MODULE_BASE.get());
                        output.accept(ModItems.MODULE_SPEED.get());
                        output.accept(ModItems.MODULE_DIGGER.get());
                        output.accept(ModItems.MODULE_SILK_TOUCH.get());
                        output.accept(ModItems.MODULE_FORTUNE.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private ModCreativeModeTabs() {}
}
