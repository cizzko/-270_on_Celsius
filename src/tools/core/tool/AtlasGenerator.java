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
    // Сколько пикселей продублировать по краям
    // Это решает проблему кровоточащих текселей
    private static final int COPY_BORDER = 1;
    // Большие текстуры в атласе это подозрительно
    private static final int SOFT_MAX_EXTENT = 1024;
    // квадратные атласы причём грани степени двойки
    private static boolean QUADRATIC = true;

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
        Path atlasPath = outputDir.resolve(atlasBaseName + Atlas.ATLAS_EXT);
        Path atlasMetaPath = outputDir.resolve(atlasBaseName + Atlas.META_EXT);

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

        if (oldHashes != null && oldHashes.size() == regionMap.size() &&
                    Files.exists(atlasPath) &&
                    Files.exists(atlasMetaPath)) {
            var byRelPath = regionMap.values().stream()
                    .collect(Collectors.toMap(r -> r.path, Function.identity()));
            boolean allMatched = true;
            for (var entry : oldHashes.entrySet()) {
                Path path = entry.getKey();
                byte[] oldHash = entry.getValue();
                var currentFile = byRelPath.get(path);
                if (currentFile == null || !Arrays.equals(oldHash, currentFile.hash)) {
                    allMatched = false;
                    break;
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

        regionMap.values().removeIf(reg -> {
            if (reg.ow() > Atlas.Region.MAX_EXTENT || reg.oh() > Atlas.Region.MAX_EXTENT) {
                log("WARNING: '" + reg.path + "' cannot be packed due to exceeding limits: " + reg.ow() + "x" + reg.oh());
                return true; // игра физически не сможет обработать такие текстуры
            }
            if (reg.ow() >= SOFT_MAX_EXTENT || reg.oh() >= SOFT_MAX_EXTENT) {
                log("WARNING: '" + reg.path + "' big enough to be in an atlas: " + reg.ow() + "x" + reg.oh());
            }
            return false;
        });

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

            while ((pos = packer.pack(region.ow(), region.oh(), COPY_BORDER)).isInvalid()) {
                int w = nextBoundary(region.ow() + COPY_BORDER * 2, packer.w);
                if (QUADRATIC) {
                    packer.resize(w, w);
                    continue;
                }

                boolean increaseW = packer.w <= packer.h;
                if (packer.w >= max && increaseW) {
                    throw new IllegalArgumentException("Image '" +
                            region.path +
                            "' is too large to pack into " + max + "x" + max);
                }
                if (increaseW) {
                    packer.resize(w, packer.h);
                } else {
                    int h = nextBoundary(region.oh() + COPY_BORDER * 2, packer.h);
                    packer.resize(packer.w, h);
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
            // Рисуем оригинал со смещением на borderSize
            var im = region.regionImage;
            gr.drawImage(im, region.rx + COPY_BORDER, region.ry + COPY_BORDER, null);

            // Дублируем верхний и нижний края
            for (int x = 0; x < im.getWidth(); x++) {
                int topRGB = im.getRGB(x, 0);
                int bottomRGB = im.getRGB(x, im.getHeight() - 1);

                for (int b = 0; b < COPY_BORDER; b++) {
                    atlasImage.setRGB(region.rx + COPY_BORDER + x, region.ry + b, topRGB);           // верх
                    atlasImage.setRGB(region.rx + COPY_BORDER + x, region.ry + COPY_BORDER + im.getHeight() + b, bottomRGB); // низ
                }
            }

            // Дублируем левый и правый края
            for (int y = 0; y < im.getHeight(); y++) {
                int leftRGB = im.getRGB(0, y);
                int rightRGB = im.getRGB(im.getWidth() - 1, y);

                for (int b = 0; b < COPY_BORDER; b++) {
                    atlasImage.setRGB(region.rx + b, region.ry + COPY_BORDER + y, leftRGB);         // лево
                    atlasImage.setRGB(region.rx + COPY_BORDER + im.getWidth() + b, region.ry + COPY_BORDER + y, rightRGB); // право
                }
            }

            // Дублируем углы
            int topLeft = im.getRGB(0, 0);
            int topRight = im.getRGB(im.getWidth() - 1, 0);
            int bottomLeft = im.getRGB(0, im.getHeight() - 1);
            int bottomRight = im.getRGB(im.getWidth() - 1, im.getHeight() - 1);

            for (int b1 = 0; b1 < COPY_BORDER; b1++) {
                for (int b2 = 0; b2 < COPY_BORDER; b2++) {
                    atlasImage.setRGB(region.rx + b1, region.ry + b2, topLeft);                           // верх-левый
                    atlasImage.setRGB(region.rx + COPY_BORDER + im.getWidth() + b1, region.ry + b2, topRight); // верх-правый
                    atlasImage.setRGB(region.rx + b1, region.ry + COPY_BORDER + im.getHeight() + b2, bottomLeft); // низ-левый
                    atlasImage.setRGB(region.rx + COPY_BORDER + im.getWidth() + b1, region.ry + COPY_BORDER + im.getHeight() + b2, bottomRight); // низ-правый
                }
            }
        }
        gr.dispose();

        Files.createDirectories(outputDir);

        ImageIO.write(atlasImage, "png", atlasPath.toFile());
        writeMetadata(atlasMetaPath, packer.w, packer.h, regions, errorRegion);
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

    private static void writeMetadata(Path file, int w, int h, ArrayList<Region> regions, Region errorRegion) throws IOException {
        try (var wr = MAPPER.createGenerator(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            wr.writeStartObject();

            wr.writeNumberField("width", w);
            wr.writeNumberField("height", h);
            wr.writeStringField("error", errorRegion.name);
            {
                wr.writeObjectFieldStart("regions");
                for (Region region : regions) {
                    wr.writeObjectFieldStart(region.name);
                    wr.writeNumberField("x", region.rx + COPY_BORDER);
                    wr.writeNumberField("y", region.ry + COPY_BORDER);
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
