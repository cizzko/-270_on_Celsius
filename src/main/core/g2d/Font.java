package core.g2d;

import it.unimi.dsi.fastutil.chars.Char2ObjectAVLTreeMap;

public final class Font {
    public static final int fontSize = 18;

    // см. AtlasGenerator
    static final int PIXEL_GAP = 1;

    Texture texture;
    Glyph[] glyphTable;
    Glyph unknownGlyph;

    Font() {}

    public Glyph getGlyph(char ch) {
        return glyphTable[ch];
    }

    public Texture texture() {
        return texture;
    }

    public static final class Glyph implements Drawable {
        private final Font font;
        private final char ch;
        private final byte width, height;

        short x, y;
        private short u, v, u2, v2;

        public Glyph(Font font, char ch,
                     byte width, byte height) {
            this.font = font;
            this.ch = ch;
            this.width = width;
            this.height = height;
        }

        void computeTextureCoordinates() {
            this.u  = BytePack.toB16((x + 0.5f) / font.texture.width());
            this.v  = BytePack.toB16((y + 0.5f) / font.texture.height());
            this.u2 = BytePack.toB16((1f * x + width) / font.texture.width());
            this.v2 = BytePack.toB16((1f * y + height) / font.texture.height());
        }

        @Override
        public short id() { return font.texture.id(); }

        public Font font() {
            return font;
        }

        public char ch() {
            return ch;
        } // TODO должно быть в идеале codepoint

        public int x() {
            return Short.toUnsignedInt(x);
        }

        public int y() {
            return Short.toUnsignedInt(y);
        }

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
            return "Glyph{'" + ch + "'}";
        }
    }
}
