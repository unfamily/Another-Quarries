package net.unfamily.another_quarries.registry;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.client.gui.QuarryMenu;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, AnotherQuarries.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<QuarryMenu>> QUARRY_MENU =
            MENUS.register("quarry", () -> new MenuType<>(QuarryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    private ModMenuTypes() {}
}
