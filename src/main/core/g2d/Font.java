package core.g2d;

import java.util.Map;

public final class Font {
    static final int fontSize = 18;

    // см. AtlasGenerator
    static final int PIXEL_GAP = 1;

    Texture texture;
    Map<Character, Glyph> glyphTable;
    Glyph unknownGlyph;

    Font() {}

    public Glyph getGlyph(char ch) {
        return glyphTable.getOrDefault(ch, unknownGlyph);
    }

    public Texture getTexture() {
        return texture;
    }

    public static final class Glyph implements Drawable {
        private final Font font;
        private final char ch;
        private final int width, height;

        int x, y;
        private float u, v, u2, v2;

        public Glyph(Font font, char ch,
                     int width, int height) {
            this.font = font;
            this.ch = ch;
            this.width = width;
            this.height = height;
        }

        void computeTextureCoordinates() {
            this.u = x / (float) font.texture.width();
            this.v = y / (float) font.texture.height();
            this.u2 = (x + width) / (float) font.texture.width();
            this.v2 = (y + height) / (float) font.texture.height();
        }

        public Font font() {
            return font;
        }

        public char ch() {
            return ch;
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
        public String toString() {
            return "Glyph{'" + ch + "'}";
        }
    }
}
