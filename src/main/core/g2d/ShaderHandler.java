package core.g2d;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.EventHandling.Config;
import core.assets.AssetHandler;
import core.assets.AssetReleaser;
import core.assets.AssetResolver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;

public final class ShaderHandler extends AssetHandler<Shader, ShaderHandler.Params, ShaderHandler.State> {
    public ShaderHandler() {
        super(Shader.class, "shaders");
    }

    @Override
    public void release(AssetReleaser rel, Shader asset) {
        asset.close();
    }

    @Override
    public void loadAsync(AssetResolver res, String name, Params params, State state) {
        state.vertSource = res.fork(() -> Files.readString(dir.resolve(params.vertFile(name)), StandardCharsets.UTF_8));
        state.fragSource = res.fork(() -> Files.readString(dir.resolve(params.fragFile(name)), StandardCharsets.UTF_8));
        state.attributesSource = res.fork(() -> {
            ObjectNode node;
            try (var is = Files.newInputStream(dir.resolve(params.metaFile(name)))) {
                node = (ObjectNode) Config.json.readTree(is);
            }
            var attributes = node.path("attributes");

            var attributesList = new ArrayList<VertexAttribute>(attributes.size());
            attributes.forEachEntry((key, value) -> {
                int size = value.required("size").asInt();
                var type = VertexAttribute.Type.valueOf(
                        value.required("type").asText().toUpperCase(Locale.ROOT));


                var formatStr = value.required("format").asText().toUpperCase(Locale.ROOT);
                var format = switch (formatStr) {
                    case "DIRECT" -> switch (type) {
                        case FLOAT -> VertexAttribute.Format.DIRECT_FLOAT;
                        case UNSIGNED_BYTE, BYTE, UNSIGNED_SHORT, SHORT, UNSIGNED_INT, INT ->
                                VertexAttribute.Format.INTEGRAL;
                    };
                    case "NORMALIZED" -> VertexAttribute.Format.NORMALIZED;
                    default -> throw new IllegalArgumentException("Unknown format: '" + formatStr + "'");
                };
                attributesList.add(new VertexAttribute(size, type, format));
            });
            var uniforms = node.path("uniforms");
            var uniformsMap = new HashMap<String, Shader.Uniform>();
            uniforms.forEachEntry((key, value) -> {
                var type = Shader.Uniform.Type.valueOf(value.required("type").asText().toUpperCase(Locale.ROOT));
                uniformsMap.put(key, new Shader.Uniform(type));
            });

            return new Attributes(new VertexFormat(
                    attributesList.toArray(new VertexAttribute[0])),
                    uniformsMap);
        });
    }

    @Override
    public Shader loadSync(AssetResolver res, String name, Params params, State state) throws Exception {
        String vertSource = res.join(state.vertSource);
        String fragSource = res.join(state.fragSource);
        var attributes = res.join(state.attributesSource);
        res.checkIfFailed();
        return Shader.load(name, vertSource, fragSource, attributes.vertexFormat, attributes.uniforms);
    }

    @Override
    protected Params createParams() {
        return new Params();
    }

    @Override
    protected State createState() {
        return new State();
    }

    public record Attributes(VertexFormat vertexFormat, Map<String, Shader.Uniform> uniforms) { }

    public static final class Params {
        public String vertFile;
        public String fragFile;
        public String metaFile;

        String vertFile(String name) { return applyDefaults(vertFile, name, Shader.VERT_EXT); }
        String fragFile(String name) { return applyDefaults(fragFile, name, Shader.FRAG_EXT); }
        String metaFile(String name) { return applyDefaults(metaFile, name, Shader.META_EXT); }

        static String applyDefaults(String name, String baseName, String ext) {
            if (name == null) {
                return baseName + ext;
            }
            if (name.indexOf('.') == -1) {
                return name + ext;
            }
            return name;
        }
    }

    public static final class State {
        Future<String> vertSource, fragSource;
        Future<Attributes> attributesSource;
    }
}
