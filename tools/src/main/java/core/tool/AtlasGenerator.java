package core.tool;

import com.google.gson.stream.JsonWriter;
import core.graphic.RectanglePacker;
import core.g2d.Atlas;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

public final class AtlasGenerator {

    private static final String IMAGE_EXT = ".png";

    static final class Region {
        final Path path;
        final String name;
        final BufferedImage regionImage;

        int rx, ry;

        public Region(Path path, String name, BufferedImage regionImage) {
            this.path = path;
            this.name = name;
            this.regionImage = regionImage;
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
        Path outputDir = basePath.resolve("out");
        Set<Path> ignore = Set.of(
                basePath.resolve("World/Other/background.png"),
                basePath.resolve("World/Sky/skyBackground0.png"),
                basePath.resolve("World/Sky/skyBackground1.png"),
                basePath.resolve("World/Sun/InterpolatedSunset.png"),
                basePath.resolve("World/Sun/nonInterpolatedSunset.png"),
                basePath.resolve("UI/GUI/modifiedTemperature.png"),
                basePath.resolve("World/Sun/sun.png")

        );
        Path error = basePath.resolve("World/textureNotFound.png");
        String baseName = "sprites"; // sptrites.atlas, sprites.atlas.meta
        process(outputDir, baseName, basePath, error, ignore, 64, 1024 * 8);
    }

    public static void process(Path outputDir,
                               String atlasBaseName,
                               Path sourceDir, Path errorImage,
                               Set<Path> ignore, int min, int max) throws IOException {
        HashMap<String, Region> regionMap = new HashMap<>();

        class WalkVisitor extends SimpleFileVisitor<Path> {
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
                    log("Loading image '" + relativePath + "'");

                    BufferedImage regionImage = ImageIO.read(path.toFile());
                    regionMap.put(regionName, new Region(relativePath, regionName, regionImage));
                }
                return FileVisitResult.CONTINUE;
            }
        }

        Files.walkFileTree(sourceDir, new WalkVisitor());

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

            while ((pos = packer.pack(region.ow(), region.oh())).isInvalid()) {
                boolean increaseW = packer.w <= packer.h;
                if (packer.w >= max && increaseW) {
                    throw new IllegalArgumentException("Image '" +
                            region.path +
                            "' is too large to pack into " + max + "x" + max);
                }
                if (increaseW) {
                    packer.resize(MathUtil.ceilNextPowerOfTwo(packer.w + 1), packer.h);
                } else {
                    packer.resize(packer.w, MathUtil.ceilNextPowerOfTwo(packer.h + 1));
                }
            }
            region.rx = pos.x();
            region.ry = pos.y();
        }

        log("Result atlas size: " + packer.w + "x" + packer.h);

        BufferedImage atlasImage = new BufferedImage(packer.w, packer.h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = atlasImage.createGraphics();
        // gr.setColor(Color.BLACK);
        // gr.fillRect(0, 0, packer.w, packer.h);

        for (Region region : regions) {
            gr.drawImage(region.regionImage, region.rx, region.ry, null);
        }
        gr.dispose();

        Files.createDirectories(outputDir);

        Path atlasPath = outputDir.resolve(atlasBaseName + Atlas.ATLAS_EXT);
        ImageIO.write(atlasImage, "png", atlasPath.toFile());

        Path atlasMetaPath = outputDir.resolve(atlasBaseName + Atlas.META_EXT);
        try (JsonWriter wr = new JsonWriter(Files.newBufferedWriter(atlasMetaPath, StandardCharsets.UTF_8))) {
            wr.beginObject();
            wr.name("error").value(errorRegion.name);
            wr.name("regions").beginObject();

            for (Region region : regions) {
                wr.name(region.name);
                wr.beginObject();
                wr.name("x").value(region.rx);
                wr.name("y").value(region.ry);
                wr.name("width").value(region.ow());
                wr.name("height").value(region.oh());
                wr.endObject();
            }
            wr.endObject();
            wr.endObject();
        }
    }

    private static void log(String str) {
        System.out.println(str);
    }
}
