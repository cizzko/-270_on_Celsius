package core.g2d;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

public final class Font {
    public static final int fontSize = 18;

    // см. AtlasGenerator
    static final int PIXEL_GAP = 1;

    Texture texture;
    Char2ObjectOpenHashMap<Glyph> glyphTable;
    Glyph unknownGlyph;

    Font() {}

    public Glyph getGlyph(char ch) {
        return glyphTable.get(ch);
    }

    public Texture texture() {
        return texture;
    }

    public static final class Glyph implements Drawable {
        private final short fontTexId;
        private final byte width, height;
        private final short u, v, u2, v2;

        public Glyph(Font font, byte width, byte height, short x, short y) {
            Texture tex = font.texture;
            this.fontTexId = tex.glHandle;
            this.width = width;
            this.height = height;

            this.u  = BytePack.toB16((x + 0.5f) / tex.width());
            this.v  = BytePack.toB16((y + 0.5f) / tex.height());
            this.u2 = BytePack.toB16((1f * x + width) / tex.width());
            this.v2 = BytePack.toB16((1f * y + height) / tex.height());
        }

        @Override
        public short id() { return fontTexId; }

        @Override
        public int width() { return Byte.toUnsignedInt(width); }

        @Override
        public int height() { return Byte.toUnsignedInt(height); }

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
        public String toString() {
            return "Glyph{glHandle=" + fontTexId + "}";
        }
    }
}
