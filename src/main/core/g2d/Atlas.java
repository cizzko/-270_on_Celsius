package core.g2d;

import it.unimi.dsi.fastutil.HashCommon;
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

    public Region errorRegion() {
        return errorRegion;
    }

    public Texture texture() {
        return texture;
    }

    public static final class Region implements Drawable {
        private final Atlas atlas;
        private final String name;
        private final short x, y;
        private final short width, height;

        public short u, v;
        public short u2, v2;

        Region(Atlas atlas, String name, short x, short y, short width, short height) {
            this.atlas = atlas;
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        void computeTextureCoordinates() {
            this.u  = BytePack.toB16((x + 0.5f) / atlas.texture.width());
            this.v  = BytePack.toB16((y + 0.5f) / atlas.texture.height());
            this.u2 = BytePack.toB16((1f * x + width) / atlas.texture.width());
            this.v2 = BytePack.toB16((1f * y + height) / atlas.texture.height());
        }

        public Atlas atlas() {
            return atlas;
        }

        public String name() {
            return name;
        }

        public int x() {
            return Short.toUnsignedInt(x);
        }

        public int y() {
            return Short.toUnsignedInt(y);
        }

        @Override
        public short id() { return atlas.texture.id(); }

        @Override
        public int width() {
            return Short.toUnsignedInt(width);
        }

        @Override
        public int height() {
            return Short.toUnsignedInt(height);
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
            return HashCommon.mix(name.hashCode());
        }

        @Override
        public String toString() {
            return "Region{" + name + '}';
        }
    }
}
