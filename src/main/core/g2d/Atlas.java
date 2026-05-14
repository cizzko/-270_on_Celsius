package core.g2d;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class Atlas {
    public static final String ATLAS_EXT = ".atlas";
    public static final String META_EXT = ATLAS_EXT + ".meta";
    public static final String HASH_EXT = ATLAS_EXT + ".hash";

    Texture texture;
    Region errorRegion;
    Map<String, Region> regions;

    public @Nullable Region find(String regionName) {
        return regions.get(regionName);
    }

    /// @deprecated рабочий, но делает много лишней работы.
    /// Лучше если все текстуры буду корректно заданы изначально
    @Deprecated
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

    public Texture getTexture() {
        return texture;
    }

    public static final class Region implements Drawable {
        private final Atlas atlas;
        private final String name;
        private final int x, y;
        private final short width, height;

        private float u, v;
        private float u2, v2;

        public Region(Atlas atlas, String name, int x, int y, int width, int height) {
            this.atlas = atlas;
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = (short) width;
            this.height = (short) height;
        }

        void computeTextureCoordinates() {
            this.u = (x + 0.5f) / (float) atlas.texture.width();
            this.v = (y + 0.5f) / (float) atlas.texture.height();
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
            if (this == o) {
                return true;
            }
            if (!(o instanceof Region region)) {
                return false;
            }
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
