package core.lang;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public sealed interface Format permits JsonFormat, PropertiesFormat {
    @SuppressWarnings("unused")
    @JsonCreator
    static Format of(String format) {
        return switch (format) {
            case "json" -> JsonFormat.INSTANCE;
            case "properties" -> PropertiesFormat.INSTANCE;
            default -> throw new IllegalArgumentException("Unknown format: '" + format + "'");
        };
    }

    String ext();

    void write(Path file, TreeMap<String, TrLine> trMap, Context ctx) throws IOException;

    TreeMap<String, TrLine> read(Path file, Context ctx) throws IOException;

    Map<String, String> readTable(Path file, Context ctx) throws IOException;
}
