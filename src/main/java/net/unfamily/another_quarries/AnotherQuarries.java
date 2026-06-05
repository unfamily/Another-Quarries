package net.unfamily.another_quarries;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.unfamily.another_quarries.registry.ModBlockEntities;
import net.unfamily.another_quarries.registry.ModBlocks;
import net.unfamily.another_quarries.registry.ModCreativeModeTabs;
import net.unfamily.another_quarries.registry.ModItems;
import net.unfamily.another_quarries.registry.ModMenuTypes;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.unfamily.another_quarries.mining.QuarryChunkTickets;

@Mod(AnotherQuarries.MOD_ID)
public final class AnotherQuarries {
    public static final String MOD_ID = "another_quarries";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AnotherQuarries(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.debug("Loading {}", MOD_ID);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, net.unfamily.another_quarries.config.ModConfig.SPEC);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
    }

    @EventBusSubscriber(modid = MOD_ID)
    public static class ModEvents {
        @net.neoforged.bus.api.SubscribeEvent
        public static void registerTicketControllers(RegisterTicketControllersEvent event) {
            event.register(QuarryChunkTickets.CONTROLLER);
        }
    }
}
