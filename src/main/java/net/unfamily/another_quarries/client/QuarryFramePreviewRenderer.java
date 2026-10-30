package net.unfamily.another_quarries.client;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * World preview of the quarry structure frame: 12 edge beams centered in the frame block cells
 * (same 6/16 cross-section as {@code structure_quarry} / AD project ducts), drawn through walls (xray).
 */
public final class QuarryFramePreviewRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarryFramePreviewRenderer.class);
    private static final AtomicBoolean TYPE_LOGGED = new AtomicBoolean();

    /** Matches structure_quarry / AD project-duct pipe body ({@code 5..11} → 6 texels). */
    private static final float PIPE_THICKNESS = 6.0f / 16.0f;

    private static final QuarryFramePreviewRenderer INSTANCE = new QuarryFramePreviewRenderer();
    private final Map<Object, FrameBorder> borders = new ConcurrentHashMap<>();
    private volatile RenderType throughWallType;

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

    public void render(RenderLevelStageEvent.AfterTranslucentBlocks event) {
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

        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        PoseStack.Pose pose = poseStack.last();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = throughWallType();
        VertexConsumer consumer = buffers.getBuffer(type);
        try {
            for (FrameBorder border : borders.values()) {
                drawCenterlinePipeFrame(consumer, pose, border.corner1, border.corner2, border.color);
            }
            buffers.endBatch(type);
        } finally {
            poseStack.popPose();
        }
    }

    private RenderType throughWallType() {
        RenderType cached = throughWallType;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            cached = throughWallType;
            if (cached != null) {
                return cached;
            }
            throughWallType = cached = createThroughWallType();
            return cached;
        }
    }

    /**
     * Same base as IskaLib {@code MarkRenderer} / 1.21.1 {@code debugFilledBox} so alpha matches
     * across versions ({@code debugQuads} blends much more opaque).
     */
    @SuppressWarnings("deprecation")
    private static RenderType createThroughWallType() {
        try {
            RenderPipeline base = RenderTypes.debugFilledBox().pipeline();
            DepthStencilState depth = new DepthStencilState(CompareOp.ALWAYS_PASS, false);
            RenderPipeline.Snippet snippet = new RenderPipeline.Snippet(
                    Optional.of(base.getVertexShader()),
                    Optional.of(base.getFragmentShader()),
                    Optional.of(base.getShaderDefines()),
                    Optional.of(base.getSamplers()),
                    Optional.of(base.getUniforms()),
                    Optional.of(base.getColorTargetState()),
                    Optional.of(depth),
                    Optional.of(base.getPolygonMode()),
                    Optional.of(base.isCull()),
                    Optional.of(base.getVertexFormat()),
                    Optional.of(base.getVertexFormatMode()));
            RenderPipeline pipeline = RenderPipeline.builder(snippet)
                    .withLocation(Identifier.parse("another_quarries:pipeline/frame_preview_xray"))
                    .build();
            RenderSetup setup = RenderSetup.builder(pipeline).createRenderSetup();
            Method create = RenderType.class.getDeclaredMethod("create", String.class, RenderSetup.class);
            create.setAccessible(true);
            return (RenderType) create.invoke(null, "another_quarries/frame_preview_xray", setup);
        } catch (Throwable t) {
            if (TYPE_LOGGED.compareAndSet(false, true)) {
                LOGGER.warn("Could not build through-wall frame preview RenderType; falling back to debugFilledBox", t);
            }
            return RenderTypes.debugFilledBox();
        }
    }

    /**
     * Beams along block centers of the inclusive corner AABB — matches where {@code structure_quarry}
     * pipes sit (model center at 0.5, body 6/16), not flush against the outer hull faces.
     */
    private static void drawCenterlinePipeFrame(VertexConsumer consumer, PoseStack.Pose pose,
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

        // Bottom ring
        drawBeam(consumer, pose, x0, y0, z0, x1, y0, z0, 0, ht, ht, red, green, blue, alpha);
        drawBeam(consumer, pose, x0, y0, z1, x1, y0, z1, 0, ht, ht, red, green, blue, alpha);
        drawBeam(consumer, pose, x0, y0, z0, x0, y0, z1, ht, ht, 0, red, green, blue, alpha);
        drawBeam(consumer, pose, x1, y0, z0, x1, y0, z1, ht, ht, 0, red, green, blue, alpha);
        // Top ring
        drawBeam(consumer, pose, x0, y1, z0, x1, y1, z0, 0, ht, ht, red, green, blue, alpha);
        drawBeam(consumer, pose, x0, y1, z1, x1, y1, z1, 0, ht, ht, red, green, blue, alpha);
        drawBeam(consumer, pose, x0, y1, z0, x0, y1, z1, ht, ht, 0, red, green, blue, alpha);
        drawBeam(consumer, pose, x1, y1, z0, x1, y1, z1, ht, ht, 0, red, green, blue, alpha);
        // Vertical pillars
        drawBeam(consumer, pose, x0, y0, z0, x0, y1, z0, ht, 0, ht, red, green, blue, alpha);
        drawBeam(consumer, pose, x1, y0, z0, x1, y1, z0, ht, 0, ht, red, green, blue, alpha);
        drawBeam(consumer, pose, x0, y0, z1, x0, y1, z1, ht, 0, ht, red, green, blue, alpha);
        drawBeam(consumer, pose, x1, y0, z1, x1, y1, z1, ht, 0, ht, red, green, blue, alpha);
    }

    private static void drawBeam(VertexConsumer consumer, PoseStack.Pose pose,
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

        drawFilledBox(consumer, pose, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
    }

    private static void drawFilledBox(VertexConsumer consumer, PoseStack.Pose pose,
                                      float minX, float minY, float minZ,
                                      float maxX, float maxY, float maxZ,
                                      float red, float green, float blue, float alpha) {
        if (maxX <= minX || maxY <= minY || maxZ <= minZ) {
            return;
        }
        consumer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha);

        consumer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha);

        consumer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha);

        consumer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha);

        consumer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha);

        consumer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha);
    }

    private record FrameBorder(BlockPos corner1, BlockPos corner2, int color, long expirationTime) {}
}
