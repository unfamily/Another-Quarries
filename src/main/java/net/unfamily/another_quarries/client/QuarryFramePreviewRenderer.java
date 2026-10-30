package net.unfamily.another_quarries.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * World preview of the quarry structure frame: 12 edge beams centered in the frame block cells
 * (same 6/16 cross-section as {@code structure_quarry} / AD project ducts), drawn through walls (xray).
 */
public final class QuarryFramePreviewRenderer {
    /** Matches structure_quarry / AD project-duct pipe body ({@code 5..11} → 6 texels). */
    private static final float PIPE_THICKNESS = 6.0f / 16.0f;

    private static final QuarryFramePreviewRenderer INSTANCE = new QuarryFramePreviewRenderer();
    private final Map<Object, FrameBorder> borders = new ConcurrentHashMap<>();

    private QuarryFramePreviewRenderer() {}

    public static QuarryFramePreviewRenderer getInstance() {
        return INSTANCE;
    }

    public void showArea(Object key, BlockPos corner1, BlockPos corner2, int colorArgb, int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || key == null || corner1 == null || corner2 == null) {
            return;
        }
        long expire = durationTicks > 0 ? mc.level.getGameTime() + durationTicks : Long.MAX_VALUE;
        borders.put(key, new FrameBorder(corner1.immutable(), corner2.immutable(), colorArgb, expire));
    }

    public void clearArea(Object key) {
        if (key != null) {
            borders.remove(key);
        }
    }

    public void clearAll() {
        borders.clear();
    }

    public void render(RenderLevelStageEvent event) {
        if (borders.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long now = mc.level.getGameTime();
        borders.entrySet().removeIf(e -> e.getValue().expirationTime <= now);
        if (borders.isEmpty()) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.debugFilledBox();
        VertexConsumer consumer = buffers.getBuffer(type);

        RenderSystem.disableDepthTest();
        try {
            for (FrameBorder border : borders.values()) {
                drawCenterlinePipeFrame(poseStack, consumer, border.corner1, border.corner2, border.color);
            }
            buffers.endBatch(type);
        } finally {
            RenderSystem.enableDepthTest();
            poseStack.popPose();
        }
    }

    /**
     * Beams along block centers of the inclusive corner AABB — matches where {@code structure_quarry}
     * pipes sit (model center at 0.5, body 6/16), not flush against the outer hull faces.
     */
    private static void drawCenterlinePipeFrame(PoseStack poseStack, VertexConsumer consumer,
                                                BlockPos a, BlockPos b, int color) {
        float x0 = Math.min(a.getX(), b.getX()) + 0.5f;
        float y0 = Math.min(a.getY(), b.getY()) + 0.5f;
        float z0 = Math.min(a.getZ(), b.getZ()) + 0.5f;
        float x1 = Math.max(a.getX(), b.getX()) + 0.5f;
        float y1 = Math.max(a.getY(), b.getY()) + 0.5f;
        float z1 = Math.max(a.getZ(), b.getZ()) + 0.5f;

        float ht = PIPE_THICKNESS * 0.5f;
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >> 24) & 0xFF) / 255.0F;

        drawBeam(poseStack, consumer, x0, y0, z0, x1, y0, z0, 0, ht, ht, red, green, blue, alpha);
        drawBeam(poseStack, consumer, x0, y0, z1, x1, y0, z1, 0, ht, ht, red, green, blue, alpha);
        drawBeam(poseStack, consumer, x0, y0, z0, x0, y0, z1, ht, ht, 0, red, green, blue, alpha);
        drawBeam(poseStack, consumer, x1, y0, z0, x1, y0, z1, ht, ht, 0, red, green, blue, alpha);

        drawBeam(poseStack, consumer, x0, y1, z0, x1, y1, z0, 0, ht, ht, red, green, blue, alpha);
        drawBeam(poseStack, consumer, x0, y1, z1, x1, y1, z1, 0, ht, ht, red, green, blue, alpha);
        drawBeam(poseStack, consumer, x0, y1, z0, x0, y1, z1, ht, ht, 0, red, green, blue, alpha);
        drawBeam(poseStack, consumer, x1, y1, z0, x1, y1, z1, ht, ht, 0, red, green, blue, alpha);

        drawBeam(poseStack, consumer, x0, y0, z0, x0, y1, z0, ht, 0, ht, red, green, blue, alpha);
        drawBeam(poseStack, consumer, x1, y0, z0, x1, y1, z0, ht, 0, ht, red, green, blue, alpha);
        drawBeam(poseStack, consumer, x0, y0, z1, x0, y1, z1, ht, 0, ht, red, green, blue, alpha);
        drawBeam(poseStack, consumer, x1, y0, z1, x1, y1, z1, ht, 0, ht, red, green, blue, alpha);
    }

    private static void drawBeam(PoseStack poseStack, VertexConsumer consumer,
                                 float x0, float y0, float z0, float x1, float y1, float z1,
                                 float hx, float hy, float hz,
                                 float red, float green, float blue, float alpha) {
        float minX = Math.min(x0, x1) - hx;
        float maxX = Math.max(x0, x1) + hx;
        float minY = Math.min(y0, y1) - hy;
        float maxY = Math.max(y0, y1) + hy;
        float minZ = Math.min(z0, z1) - hz;
        float maxZ = Math.max(z0, z1) + hz;

        if (maxX - minX < PIPE_THICKNESS && hx > 0) {
            float mid = (minX + maxX) * 0.5f;
            minX = mid - hx;
            maxX = mid + hx;
        }
        if (maxY - minY < PIPE_THICKNESS && hy > 0) {
            float mid = (minY + maxY) * 0.5f;
            minY = mid - hy;
            maxY = mid + hy;
        }
        if (maxZ - minZ < PIPE_THICKNESS && hz > 0) {
            float mid = (minZ + maxZ) * 0.5f;
            minZ = mid - hz;
            maxZ = mid + hz;
        }

        LevelRenderer.addChainedFilledBoxVertices(
                poseStack, consumer, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
    }

    private record FrameBorder(BlockPos corner1, BlockPos corner2, int color, long expirationTime) {}
}
