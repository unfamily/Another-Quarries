package net.unfamily.another_quarries.client;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.math.Transformation;

import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import net.neoforged.neoforge.client.model.quad.QuadTransforms;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.registry.ModBlocks;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Baked quads for structure_quarry composite templates ({@code structure_quarry_default} + {@code structure_quarry_line}).
 */
public final class CompositeGeometry {
    private static final Gson ELEMENT_GSON = new GsonBuilder()
            .registerTypeAdapter(CuboidModelElement.class, new CuboidModelElement.Deserializer())
            .registerTypeAdapter(CuboidFace.class, new CuboidFace.Deserializer())
            .create();
    private static final ModelDebugName DEBUG_NAME = () -> "structure_quarry_composite";

    private final Map<String, List<BakedQuad>> defaultByName;
    private final List<BakedQuad> lineCenterQuadsIdentity;
    /** All quads from the line model (center + end caps), for item display — same as AD {@code DuctCompositeGeometry#lineAllQuads}. */
    private final List<BakedQuad> lineAllQuadsIdentity;
    private final boolean built;

    private CompositeGeometry(
            Map<String, List<BakedQuad>> defaultByName,
            List<BakedQuad> lineCenterQuads,
            List<BakedQuad> lineAllQuads,
            boolean built) {
        this.defaultByName = defaultByName;
        this.lineCenterQuadsIdentity = lineCenterQuads;
        this.lineAllQuadsIdentity = lineAllQuads;
        this.built = built;
    }

    public static CompositeGeometry bake(
            Function<Identifier, TextureAtlasSprite> textureGetter,
            Identifier modelDefaultId,
            Identifier modelLineId,
            Identifier textureId) {
        ModelBaker baker = CompositeModelBaker.fromTextureGetter(textureGetter);
        Map<String, List<BakedQuad>> byName = new HashMap<>();
        List<BakedQuad> lineCenter = List.of();
        try {
            String textureRef = textureId.toString();
            ParsedElements defParsed = readElements(modelDefaultId, textureRef);
            ParsedElements lineParsed = readElements(modelLineId, textureRef);
            ModelState identity = BlockModelRotation.IDENTITY;
            var materialGetter = materialGetter(baker, textureId);

            for (int i = 0; i < defParsed.elements().size(); i++) {
                String name = i < defParsed.elementNames().size() ? defParsed.elementNames().get(i) : null;
                if (name == null) {
                    continue;
                }
                List<BakedQuad> quads = UnbakedElementsHelper.bakeElements(
                        baker,
                        List.of(defParsed.elements().get(i)),
                        materialGetter,
                        identity);
                byName.put(name, quads);
            }

            List<CuboidModelElement> lineCenterElements = new ArrayList<>();
            for (int i = 0; i < lineParsed.elements().size(); i++) {
                if (i < lineParsed.elementNames().size() && "center".equals(lineParsed.elementNames().get(i))) {
                    lineCenterElements.add(lineParsed.elements().get(i));
                }
            }
            lineCenter = UnbakedElementsHelper.bakeElements(baker, lineCenterElements, materialGetter, identity);
            List<BakedQuad> lineAll = UnbakedElementsHelper.bakeElements(baker, lineParsed.elements(), materialGetter, identity);
            return new CompositeGeometry(Map.copyOf(byName), lineCenter, List.copyOf(lineAll), true);
        } catch (Exception ex) {
            AnotherQuarries.LOGGER.error(
                    "Failed to bake structure composite geometry (default={}, line={})",
                    modelDefaultId,
                    modelLineId,
                    ex);
            return new CompositeGeometry(Map.of(), List.of(), List.of(), false);
        }
    }

    private static java.util.function.Function<String, Material.Baked> materialGetter(ModelBaker baker, Identifier textureId) {
        return ref -> {
            Identifier resolved = ref.startsWith("#") ? textureId : Identifier.parse(ref);
            return baker.materials().get(new Material(resolved), DEBUG_NAME);
        };
    }

    private static String classpathModelPath(Identifier modelId) {
        return "/assets/" + modelId.getNamespace() + "/models/" + modelId.getPath() + ".json";
    }

    private static void resolveFaceTextures(JsonObject root, String textureRlString) {
        if (!root.has("elements")) {
            return;
        }
        JsonArray elements = root.getAsJsonArray("elements");
        for (JsonElement el : elements) {
            if (!el.isJsonObject() || !el.getAsJsonObject().has("faces")) {
                continue;
            }
            JsonObject faces = el.getAsJsonObject().getAsJsonObject("faces");
            for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
                if (!face.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject fo = face.getValue().getAsJsonObject();
                if (fo.has("texture") && fo.get("texture").getAsString().startsWith("#")) {
                    fo.addProperty("texture", textureRlString);
                }
            }
        }
    }

    private record ParsedElements(List<CuboidModelElement> elements, List<String> elementNames) {}

    private static List<String> extractElementNames(JsonObject root) {
        if (!root.has("elements")) {
            return List.of();
        }
        JsonArray elements = root.getAsJsonArray("elements");
        List<String> names = new ArrayList<>(elements.size());
        for (JsonElement el : elements) {
            if (el.isJsonObject() && el.getAsJsonObject().has("name")) {
                names.add(el.getAsJsonObject().get("name").getAsString());
            } else {
                names.add(null);
            }
        }
        return names;
    }

    private static ParsedElements readElements(Identifier modelId, String textureRlString) throws Exception {
        String cp = classpathModelPath(modelId);
        var stream = ModBlocks.class.getResourceAsStream(cp);
        if (stream == null) {
            throw new IllegalStateException("Missing model resource: " + modelId + " (" + cp + ")");
        }
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            if (obj.has("textures")) {
                JsonObject tex = obj.getAsJsonObject("textures");
                tex.remove("render_type");
                tex.addProperty("0", textureRlString);
                tex.addProperty("particle", textureRlString);
            }
            resolveFaceTextures(obj, textureRlString);
            List<String> elementNames = extractElementNames(obj);
            List<CuboidModelElement> elements = new ArrayList<>();
            if (obj.has("elements")) {
                for (JsonElement el : obj.getAsJsonArray("elements")) {
                    elements.add(ELEMENT_GSON.fromJson(el, CuboidModelElement.class));
                }
            }
            return new ParsedElements(Collections.unmodifiableList(elements), Collections.unmodifiableList(elementNames));
        }
    }

    public boolean isBuilt() {
        return built;
    }

    public List<BakedQuad> quadsNamed(String name) {
        return defaultByName.getOrDefault(name, List.of());
    }

    public List<BakedQuad> lineCenterQuads() {
        return lineCenterQuadsIdentity;
    }

    public List<BakedQuad> lineAllQuads() {
        return lineAllQuadsIdentity;
    }

    public List<BakedQuad> transformQuads(List<BakedQuad> source, Transformation transform) {
        if (transform == null || transform.isIdentity()) {
            return List.copyOf(source);
        }
        List<BakedQuad> out = new ArrayList<>(source.size());
        for (BakedQuad q : source) {
            out.add(QuadTransforms.applyTransformation(q, transform));
        }
        return out;
    }

    public static Transformation rotationForLineAxis(Direction.Axis axis) {
        Matrix4f m = new Matrix4f();
        m.translation(0.5f, 0.5f, 0.5f);
        switch (axis) {
            case X -> m.rotate(new Quaternionf().rotateY((float) (-Math.PI / 2)));
            case Y -> m.rotate(new Quaternionf().rotateX((float) (Math.PI / 2)));
            case Z -> {}
        }
        m.translate(-0.5f, -0.5f, -0.5f);
        return new Transformation(m);
    }

    public static String connectionPiece(Direction dir) {
        return switch (dir) {
            case UP -> "con_N";
            case DOWN -> "con_D";
            case NORTH -> "con_U";
            case SOUTH -> "con_S";
            case EAST -> "con_E";
            case WEST -> "con_W";
        };
    }

    public static String nodePiece(Direction dir) {
        return switch (dir) {
            case UP -> "node_U";
            case DOWN -> "node_D";
            case NORTH -> "node_N";
            case SOUTH -> "node_S";
            case EAST -> "node_E";
            case WEST -> "node_W";
        };
    }

    public void appendForWorld(List<BakedQuad> out, int pipeMask, int storageMask) {
        if (!built) {
            return;
        }
        DuctConnectionShape shape = DuctConnectionShape.classify(pipeMask, storageMask);
        switch (shape) {
            case SINGLE -> out.addAll(quadsNamed("center"));
            case PARTIAL -> {
                out.addAll(quadsNamed("center"));
                for (Direction d : Direction.values()) {
                    int bit = 1 << d.ordinal();
                    if ((pipeMask & bit) != 0) {
                        out.addAll(quadsNamed(connectionPiece(d)));
                    }
                    if ((storageMask & bit) != 0) {
                        out.addAll(quadsNamed(connectionPiece(d)));
                        out.addAll(quadsNamed(nodePiece(d)));
                    }
                }
            }
            case LINE_X, LINE_Y, LINE_Z -> {
                Transformation tr = rotationForLineAxis(shape.lineAxis());
                out.addAll(transformQuads(lineCenterQuads(), tr));
                Direction na = shape.lineEndNegative();
                Direction pb = shape.lineEndPositive();
                if ((storageMask & (1 << na.ordinal())) != 0) {
                    out.addAll(quadsNamed(nodePiece(na)));
                }
                if ((storageMask & (1 << pb.ordinal())) != 0) {
                    out.addAll(quadsNamed(nodePiece(pb)));
                }
            }
            case MULTI -> {
                out.addAll(quadsNamed("center"));
                for (Direction d : Direction.values()) {
                    int bit = 1 << d.ordinal();
                    if ((pipeMask & bit) != 0) {
                        out.addAll(quadsNamed(connectionPiece(d)));
                    }
                    if ((storageMask & bit) != 0) {
                        out.addAll(quadsNamed(connectionPiece(d)));
                    }
                }
                appendStorageNodesOnly(out, storageMask);
            }
        }
    }

    private void appendStorageNodesOnly(List<BakedQuad> out, int storageMask) {
        for (Direction d : Direction.values()) {
            if ((storageMask & (1 << d.ordinal())) != 0) {
                out.addAll(quadsNamed(nodePiece(d)));
            }
        }
    }
}
