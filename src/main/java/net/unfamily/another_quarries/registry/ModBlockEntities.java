package net.unfamily.another_quarries.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.block.entity.QuarryBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AnotherQuarries.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuarryBlockEntity>> QUARRY_BE =
            BLOCK_ENTITIES.register("quarry",
                    () -> new BlockEntityType<>(QuarryBlockEntity::new, ModBlocks.QUARRY.get()));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

    @EventBusSubscriber(modid = AnotherQuarries.MOD_ID)
    public static class CapabilityRegistration {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(
                    Capabilities.Energy.BLOCK,
                    QUARRY_BE.get(),
                    (be, side) -> be.getEnergyHandler());
            event.registerBlockEntity(
                    Capabilities.Item.BLOCK,
                    QUARRY_BE.get(),
                    (be, side) -> be.getItemTransferHandler());
        }
    }

    private ModBlockEntities() {}
}
