package core.g2d;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.Global;

import javax.swing.plaf.synth.Region;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class Atlas {
    public static final String ATLAS_EXT = ".atlas";
    public static final String META_EXT = ATLAS_EXT + ".meta";

    private Texture texture;
    private Region errorRegion;
    private Map<String, Region> regions;

    public static Atlas load(String atlasBaseName) throws IOException {
        Texture texture = Texture.load(atlasBaseName + ATLAS_EXT);
        Atlas atlas = new Atlas();
        atlas.texture = texture;

        JsonObject meta;
        try (var reader = Global.assets.resourceReader(atlasBaseName + META_EXT)) {
            meta = JsonParser.parseReader(reader)
                    .getAsJsonObject();
        }

        HashMap<String, Region> tmpRegions = new HashMap<>();
        meta.getAsJsonObject("regions").asMap().forEach((regionName, regMeta) -> {
            JsonObject regionObject = regMeta.getAsJsonObject();
            int x = regionObject.get("x").getAsInt();
            int y = regionObject.get("y").getAsInt();
            int width = regionObject.get("width").getAsInt();
            int height = regionObject.get("height").getAsInt();
            tmpRegions.put(regionName, new Region(atlas, regionName, x, y, width, height));
        });
        String errorRegionName = meta.get("error").getAsString();

        Map<String, Region> regions = Map.copyOf(tmpRegions);
        Region errorRegion = regions.get(errorRegionName);
        if (errorRegion == null) {
            throw new IllegalArgumentException("No error region");
        }

        atlas.regions = regions;
        atlas.errorRegion = errorRegion;
        return atlas;
    }

    public Texture getTexture() {
        return texture;
    }

    // @Nullable
    public Region find(String regionName) {
        return regions.get(regionName);
    }

    public Region byPath(String regionName) {
        if (regionName == null) {
            return errorRegion;
        }
        regionName = regionName.replace('\\', '/');

        if (regionName.endsWith(".png")) {
            regionName = regionName.substring(0, regionName.length() - ".png".length());
        }
        if (regionName.startsWith("/")) {
            regionName = regionName.substring(1);
        }
        return regions.getOrDefault(regionName, errorRegion);
    }

    public Region get(String regionName) {
        return regions.getOrDefault(regionName, errorRegion);
    }

    public Region getErrorRegion() {
        return errorRegion;
    }

    public static final class Region implements Drawable {
        private final Atlas atlas;
        private final String name;
        private final int x, y;
        private final int width, height;
        private final float u, v;
        private final float u2, v2;

        public Region(Atlas atlas, String name, int x, int y, int width, int height) {
            this.atlas = atlas;
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.u = x / (float) atlas.texture.width();
            this.v = y / (float) atlas.texture.height();
            this.u2 = (x + width) / (float) atlas.texture.width();
            this.v2 = (y + height) / (float) atlas.texture.height();
        }

        public Atlas atlas() {
            return atlas;
        }

        public String name() {
            return name;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        @Override
        public int width() {
            return width;
        }

        @Override
        public int height() {
            return height;
        }

        @Override
        public float u() {
            return u;
        }

        @Override
        public float v() {
            return v;
        }

        @Override
        public float u2() {
            return u2;
        }

        @Override
        public float v2() {
            return v2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Region region)) return false;
            return name.equals(region.name);
        }

        @Override
        public int hashCode() {
            int h = 5381;
            h += (h << 5) + name.hashCode();
            return h;
        }

        @Override
        public String toString() {
            return "Region{" + name + '}';
        }
    }
}
