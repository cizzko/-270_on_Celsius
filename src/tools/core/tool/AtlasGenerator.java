package core.tool;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.g2d.Atlas;
import core.graphic.RectanglePacker;
import core.math.MathUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AtlasGenerator {

    private static final String IMAGE_EXT = ".png";
    // Это способ исправления проблем с мерцающими текстурами.
    // Поскольку мерцания происходят при смене кадров и причём при определённых действиях, то
    // скорее всего это ошибка округления текстурных координат.
    // Что-то типа наслаивания (?)
    private static final int PIXEL_GAP = 2;
    // Максимальный размер по одной из осей для текстуры
    public static int MAX_EXTENT = 1024;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static final class Region {
        final Path path;
        final String name;
        BufferedImage regionImage;
        final byte[] hash;

        int rx, ry;

        public Region(Path path, String name, byte[] hash) {
            this.path = path;
            this.name = name;
            this.hash = hash;
        }

        int size() {
            return ow() * oh();
        }

        int ow() {
            return regionImage.getWidth();
        }

        int oh() {
            return regionImage.getHeight();
        }
    }

    public static void main(String[] args) throws IOException {
        Path basePath = Path.of("src/assets/");
        Path outputDir = Path.of("src/assets/");

        Set<Path> ignore = Set.of(
                basePath.resolve("World/Other/background.png"),
                basePath.resolve("World/Sky/skyBackground0.png"),
                basePath.resolve("World/Sky/skyBackground1.png"),
                basePath.resolve("World/Sun/InterpolatedSunset.png"),
                basePath.resolve("World/Sun/nonInterpolatedSunset.png"),
                basePath.resolve("UI/GUI/modifiedTemperature.png"),
                basePath.resolve("World/Sun/sun.png"),
                basePath.resolve("World/Backdrops/back.png"),
                basePath.resolve("worldImage.png")
        );
        Path error = basePath.resolve("World/textureNotFound.png");
        String baseName = "sprites"; // sprites.atlas, sprites.atlas.meta
        process(outputDir, baseName, basePath, error, ignore, 64, 1024 * 8);
    }

    public static void process(Path outputDir,
                               String atlasBaseName,
                               Path sourceDir, Path errorImage,
                               Set<Path> ignore, int min, int max) throws IOException {
        long beginTs = System.currentTimeMillis();

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        HashMap<String, Region> regionMap = new HashMap<>();

        Path atlasHashPath = outputDir.resolve(atlasBaseName + Atlas.HASH_EXT);
        HashMap<Path, byte[]> oldHashes;
        if (Files.exists(atlasHashPath)) {
            oldHashes = readHash(atlasHashPath);
        } else {
            oldHashes = null;
        }

        class WalkVisitor extends SimpleFileVisitor<Path> {

            byte[] buf;

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (ignore.contains(path)) {
                    return FileVisitResult.CONTINUE;
                }

                if (path.toString().endsWith(IMAGE_EXT)) {
                    Path relativePath = sourceDir.relativize(path);
                    String filename = relativePath.toString();
                    String regionName = filename.substring(0, filename.length() - IMAGE_EXT.length())
                            .replace('\\', '/');

                    Region duplicate = regionMap.get(regionName);
                    if (duplicate != null) {
                        throw new IllegalStateException("Duplicate region name: '" +
                                relativePath + "' and '" +
                                duplicate.path + "'");
                    }

                    if (buf == null) {
                        buf = new byte[32 * 1024];
                    }

                    digest.reset();
                    try (var fis = Files.newInputStream(path)) {
                        int n;
                        while ((n = fis.read(buf)) > 0) {
                            digest.update(buf, 0, n);
                        }
                    }
                    byte[] hash = digest.digest();
                    regionMap.put(regionName, new Region(relativePath, regionName, hash));
                }
                return FileVisitResult.CONTINUE;
            }
        }

        Files.walkFileTree(sourceDir, new WalkVisitor());

        if (oldHashes != null && oldHashes.size() == regionMap.size()) {
            var byRelPath = regionMap.values().stream()
                    .collect(Collectors.toMap(r -> r.path, Function.identity()));
            boolean allMatched = true;
            for (var entry : oldHashes.entrySet()) {
                Path path = entry.getKey();
                byte[] oldHash = entry.getValue();
                var currentFile = byRelPath.get(path);
                if (currentFile == null || !Arrays.equals(oldHash, currentFile.hash)) {
                    allMatched = false;
                }
            }

            if (allMatched) {
                // Хеши файлов совпали, а также мы знаем, что никакой файл не был удалён.
                // Пожалуй, сегодня не будем делать атлас...
                log("Skipping atlas processing. All files are identical");
                log("Processing time: " + ((System.currentTimeMillis() - beginTs)/1000f) + "s");
                return;
            }
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Region reg : regionMap.values()) {
                executor.submit(() -> {
                    log("Loading image '" + reg.path + "'");
                    reg.regionImage = ImageIO.read(sourceDir.resolve(reg.path).toFile());
                    return null;
                });
            }
        }

        for (Region reg : regionMap.values()) {
            if (reg.ow() >= MAX_EXTENT || reg.oh() >= MAX_EXTENT) {
                log("WARNING: '" + reg.path + "' is too big for packing");
            }
        }

        ArrayList<Region> regions = new ArrayList<>(regionMap.values());

        Path errorPathRelative = sourceDir.relativize(errorImage);
        Region errorRegion = regions.stream()
                .filter(rg -> rg.path.equals(errorPathRelative))
                .findAny()
                .orElseThrow();

        regions.sort(Comparator.comparingInt(Region::size).reversed());

        RectanglePacker packer = new RectanglePacker(regions.size(), min, min);
        for (Region region : regions) {
            RectanglePacker.Position pos;

            while ((pos = packer.pack(region.ow(), region.oh(), PIXEL_GAP)).isInvalid()) {
                boolean increaseW = packer.w <= packer.h;
                if (packer.w >= max && increaseW) {
                    throw new IllegalArgumentException("Image '" +
                            region.path +
                            "' is too large to pack into " + max + "x" + max);
                }
                if (increaseW) {
                    packer.resize(nextBoundary(region.ow(), packer.w), packer.h);
                } else {
                    packer.resize(packer.w, nextBoundary(region.oh(), packer.h));
                }
            }
            region.rx = pos.x;
            region.ry = pos.y;
        }

        log("Result atlas size: " + packer.w + "x" + packer.h);
        log("Processing time: " + ((System.currentTimeMillis() - beginTs)/1000f) + "s");

        BufferedImage atlasImage = new BufferedImage(packer.w, packer.h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = atlasImage.createGraphics();
        for (Region region : regions) {
            gr.drawImage(region.regionImage, region.rx, region.ry, null);
        }
        gr.dispose();

        Files.createDirectories(outputDir);

        Path atlasPath = outputDir.resolve(atlasBaseName + Atlas.ATLAS_EXT);
        ImageIO.write(atlasImage, "png", atlasPath.toFile());

        Path atlasMetaPath = outputDir.resolve(atlasBaseName + Atlas.META_EXT);
        writeMetadata(atlasMetaPath, regions, errorRegion);

        writeHash(atlasHashPath, regions);
    }

    private static void writeHash(Path file, ArrayList<Region> regions) throws IOException {
        try (var wr = MAPPER.createGenerator(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            wr.writeStartObject();
            for (Region region : regions) {
                wr.writeStringField(region.path.toString(), HexFormat.of().formatHex(region.hash));
            }
            wr.writeEndObject();
        }
    }

    private static void writeMetadata(Path file, ArrayList<Region> regions, Region errorRegion) throws IOException {
        try (var wr = MAPPER.createGenerator(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            wr.writeStartObject();

            wr.writeStringField("error", errorRegion.name);
            {
                wr.writeObjectFieldStart("regions");
                for (Region region : regions) {
                    wr.writeObjectFieldStart(region.name);
                    wr.writeNumberField("x", region.rx);
                    wr.writeNumberField("y", region.ry);
                    wr.writeNumberField("width", region.ow());
                    wr.writeNumberField("height", region.oh());
                    wr.writeEndObject();
                }
                wr.writeEndObject();
            }

            wr.writeEndObject();

            wr.flush();
        }
    }

    private static int nextBoundary(int ob, int b) {
        if (true) {
            return MathUtil.ceilNextPowerOfTwo(b + 1);
        }
        return ob + b;
    }

    private static void log(String str) {
        System.out.println(str);
    }

    private static HashMap<Path, byte[]> readHash(Path file) throws IOException {
        /*
        {
          "UI/GUI/workbenchMenu/menuFull.png":"9ddd2716ed592afe05bba37343ae5f00da287b70043c6a86c437b61f4b710ff7",
          ...
        }
         */
        var result = new HashMap<Path, byte[]>();
        try (var p = MAPPER.getFactory().createParser(file.toFile())) {
            // Здесь намерено не проверяются START_OBJECT,END_OBJECT и так далее
            JsonToken t;
            while ((t = p.nextToken()) != null) {
                if (t == JsonToken.FIELD_NAME) {
                    String key = p.currentName();
                    var valueToken = p.nextToken();
                    if (valueToken == JsonToken.VALUE_STRING) {
                        result.put(Path.of(key), HexFormat.of().parseHex(p.getValueAsString()));
                    } else {
                        throw new IOException("Unexpected value by key '" + key + "' with type " + valueToken);
                    }
                }
            }
        }
        return result;
    }
}
