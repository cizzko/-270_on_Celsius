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

    /// @param regionName Точное имя в атласе с юниксовым '/' вместо виндовского. Без расширения.
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
        public static final int MAX_EXTENT = Short.MAX_VALUE;

        private final Atlas atlas;
        private final String name;
        private final short x, y;
        private final short width, height;
        public final short u, v;
        public final short u2, v2;

        Region(Atlas atlas, String name,
               short x, short y,
               short width, short height,
               int atlasWidth, int atlasHeight) {
            this.atlas = atlas;
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;

            // Чтение текстуры происходит асинхронно и поэтому atlas.texture.width() недоступен
            this.u  = BytePack.toB16((x + 0.5f) / atlasWidth);
            this.v  = BytePack.toB16((y + 0.5f) / atlasHeight);
            this.u2 = BytePack.toB16((1f * x + width) / atlasWidth);
            this.v2 = BytePack.toB16((1f * y + height) / atlasHeight);
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
