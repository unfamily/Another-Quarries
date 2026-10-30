package net.unfamily.another_quarries.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.common.NeoForge;
import net.unfamily.another_quarries.AnotherQuarries;

@EventBusSubscriber(modid = AnotherQuarries.MOD_ID, value = Dist.CLIENT)
public final class QuarryFramePreviewClientHooks {
    private static volatile boolean registered;

    private QuarryFramePreviewClientHooks() {}

    /** Ensure game-bus registration even if annotation wiring differs by NeoForge version. */
    public static void registerIfNeeded() {
        if (registered) {
            return;
        }
        synchronized (QuarryFramePreviewClientHooks.class) {
            if (registered) {
                return;
            }
            NeoForge.EVENT_BUS.register(QuarryFramePreviewClientHooks.class);
            registered = true;
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        QuarryFramePreviewRenderer.getInstance().render(event);
    }
}
