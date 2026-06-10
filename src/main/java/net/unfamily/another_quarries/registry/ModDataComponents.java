package net.unfamily.another_quarries.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.another_quarries.AnotherQuarries;

import java.util.ArrayList;
import java.util.List;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, AnotherQuarries.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> DESTROY_FILTERS =
            DATA_COMPONENTS.registerComponentType("destroy_filters", builder -> builder
                    .persistent(Codec.list(Codec.STRING))
                    .networkSynchronized(ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)));

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }

    private ModDataComponents() {}
}
