package core.tool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ConvertTypesToJson {

    private static final String PROPERTIES_EXT = ".properties";
    private static final Path sourceDir = Path.of("src/assets/World/");
    private static final Path ITEMS_DIR = Path.of("src/assets/World/ItemsCharacteristics");

    private static HashMap<String, String> itemPathToIds = new HashMap<>();

    private static void log(String str) {
        System.out.println(str);
    }

    public static void main(String[] args) throws IOException {
        var files = new ArrayList<Path>();
        class WalkVisitor extends SimpleFileVisitor<Path> {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (path.toString().endsWith(PROPERTIES_EXT)) {
                    files.add(path);
                }
                return FileVisitResult.CONTINUE;
            }
        }

        Files.walkFileTree(sourceDir, new WalkVisitor());

        var json = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        for (Path file : files) {
            String fullFileName = file.getFileName().toString();
            // redHammer.json -> redHammer -> red-hammer
            String fileName = fullFileName.replace(".properties", "");
            String id = camel2Snake(fileName);
            itemPathToIds.put(canonicalPath(file), id);
        }

        try (var threadPool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Path file : files) {
                threadPool.submit(() -> convertToJson(sourceDir, file, json));
            }
            var indexFile = ITEMS_DIR.resolve("index.json");
            try (var wr = json.newJsonWriter(Files.newBufferedWriter(indexFile, StandardCharsets.UTF_8))) {
                wr.beginArray();
                for (Path file : files) {
                    if (file.startsWith(ITEMS_DIR) && !file.endsWith("DefaultBuildMenuItems.properties")) {
                        wr.value(ITEMS_DIR.relativize(file).toString().replace(PROPERTIES_EXT, ""));
                    }
                }
                wr.endArray();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static String camel2Snake(String str) {
        StringBuilder result = new StringBuilder(str.length());
        char c = str.charAt(0);
        result.append(Character.toLowerCase(c));

        for (int i = 1; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('-');
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static void convertToJson(Path sourceDir, Path file, Gson json) {
        var prop = new Properties();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            prop.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String fullFileName = file.getFileName().toString();
        var jsonFile = file.resolveSibling(fullFileName
                .replace(PROPERTIES_EXT, ".json"));

        try (var wr = json.newJsonWriter(Files.newBufferedWriter(jsonFile, StandardCharsets.UTF_8))) {
            wr.beginObject();
            String id = getId(canonicalPath(file));
            wr.name("id").value(id);

            if (file.startsWith(ITEMS_DIR) &&
                    !fullFileName.replace(".properties", "")
                            .equalsIgnoreCase("defaultbuildmenuitems")) {

                var categoryDir = file.getName(sourceDir.getNameCount() + 1);
                String parentDir = categoryDir.toString().toLowerCase(Locale.ROOT)
                        .replace(PROPERTIES_EXT, "");

                String classType = switch (parentDir) {
                    case "factories", "blocks" -> "block";
                    case "details" -> "detail";
                    case "tools" -> "tool";
                    default -> throw new IllegalStateException("Unexpected value: " + parentDir);
                };

                wr.name("class-type").value(classType);
            }

            outer:
            for (var entry : prop.entrySet()) {
                var k = (String) entry.getKey();
                var v = (String) entry.getValue();


                for (PropertyMapping propx : PROPERTIES) {
                    if (propx.oldName.equals(k)) {
                        wr.name(propx.newName);
                        propx.converter.process(wr, v);
                        continue outer;
                    }
                }

                wr.name(k);

                switch (v) {
                    case "true" -> wr.value(true);
                    case "false" -> wr.value(false);
                    default -> {
                        if (v.endsWith("f")) {
                            try {
                                float num = Float.parseFloat(v);
                                wr.value(num);
                            } catch (NumberFormatException e) {
                                v = normalizePath(v);
                                wr.value(v);
                            }
                        } else {
                            try {
                                long num = Long.parseLong(v);
                                wr.value(num);
                            } catch (NumberFormatException e) {
                                v = normalizePath(v);

                                wr.value(v);
                            }
                        }

                    }
                }
            }

            wr.endObject();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    interface Converter {
        void process(JsonWriter wr, String val) throws IOException;
    }

    static void itemStackConverter(JsonWriter wr, String v) throws IOException {
        wr.beginObject();
        var counts = Arrays.stream(v.split(","))
                .map(ConvertTypesToJson::normalizePath)
                .collect(Collectors.groupingBy(ConvertTypesToJson::getId, Collectors.counting()));
        for (var e : counts.entrySet()) {
            String itemPath = e.getKey();
            int count = Math.toIntExact(counts.get(itemPath));

            wr.name(itemPath).value(count);
        }

        wr.endObject();
    }

    static void texturePathConverter(JsonWriter wr, String v) throws IOException {
        v = normalizePath(v);
        if (v.endsWith(".png") || v.endsWith(".gif")) {
            v = v.substring(0, v.length() - 4);
        }
        wr.value(v);
    }

    record PropertyMapping(String oldName, String newName, Converter converter) {}

    static PropertyMapping prop(String oldName, String newName, Converter converter) {
        return new PropertyMapping(oldName, newName, converter);
    }

    static final List<PropertyMapping> PROPERTIES = List.of(
            prop("Path", "texture", ConvertTypesToJson::texturePathConverter),
            prop("RequiredForBuild", "requirements", ConvertTypesToJson::itemStackConverter),
            prop("InputObjects", "input", ConvertTypesToJson::itemStackConverter),
            prop("OutputObjects", "output", ConvertTypesToJson::itemStackConverter),
            prop("Fuel", "fuel", ConvertTypesToJson::itemStackConverter),
            prop("MaxHp", "max-hp", ConvertTypesToJson::integerConverter)
    );

    static final List<String> removed = List.of("Name");

    private static void integerConverter(JsonWriter wr, String v) throws IOException {
        wr.value(Integer.parseInt(v));
    }


    public static String canonicalPath(Path file) {
        return ITEMS_DIR.relativize(file)
                .resolveSibling(file.getFileName().toString().replace(PROPERTIES_EXT, ""))
                .toString();
    }

    private static String getId(String file) {
        var id = itemPathToIds.get(file);
        Objects.requireNonNull(id, file::toString);
        return id;
    }

    private static String normalizePath(String v) {
        // Потому что \\ выглядит ужасно
        String unixLike = v.replace("\\", "/");
        // Пути вида /World/Blocks/dirt.png тоже выглядят странно, будто мы к руту обращаемся)
        if (unixLike.startsWith("/")) {
            unixLike = unixLike.substring(1);
        }
        return unixLike;
    }
}
