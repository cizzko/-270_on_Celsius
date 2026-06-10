package core.lang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

enum JsonFormat implements Format {
    INSTANCE;

    public String ext() {
        return "json";
    }

    public void write(Path file, TreeMap<String, TrLine> trMap, Context ctx) throws IOException {
        try (var gen = ctx.objectMapper().writerWithDefaultPrettyPrinter().createGenerator(Files.newOutputStream(file))) {
            gen.writeStartObject();
            for (var entry : trMap.entrySet()) {
                String key = entry.getKey();
                TrLine trLine = entry.getValue();
                if (trLine.comments().isEmpty()) {
                    gen.writeStringField(key, trLine.text());
                } else {
                    gen.writeObjectField(key, trLine);
                }
            }
            gen.writeEndObject();
        }
    }

    public TreeMap<String, TrLine> read(Path file, Context ctx) throws IOException {
        return readTable0(file, ctx, TrMap::makeTrMap, JsonFormat::trLineMapper);
    }

    public Map<String, String> readTable(Path file, Context ctx) throws IOException {
        return Map.copyOf(readTable0(file, ctx, HashMap::new, JsonFormat::lineMapper));
    }

    private static String lineMapper(JsonNode trLineNode, Context ctx) {
        if (trLineNode.isObject()) {
            return trLineNode.required("text").asText();
        }
        return trLineNode.asText();
    }

    private static TrLine trLineMapper(JsonNode trLineNode, Context ctx) {
        if (trLineNode.isObject()) {
            try {
                return ctx.objectMapper().treeToValue(trLineNode, TrLine.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return new TrLine(trLineNode.asText(), List.of());
    }

    private static <M extends Map<String, V>, V> M readTable0(Path file, Context ctx,
                                                              Supplier<M> mapConstr,
                                                              BiFunction<JsonNode, Context, @Nullable V> mapper) throws IOException {
        ObjectNode objectNode;
        try (var is = Files.newInputStream(file)) {
            objectNode = (ObjectNode) ctx.objectMapper().readTree(is);
        }
        var trMap = mapConstr.get();
        objectNode.forEachEntry((key, trLineNode) -> {
            var line = mapper.apply(trLineNode, ctx);
            if (line != null) {
                trMap.put(key, line);
            }
        });
        return trMap;
    }
}
