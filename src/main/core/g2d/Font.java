package core.g2d;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

public final class Font {
    public static final int fontSize = 22;

    // см. AtlasGenerator
    static final int PIXEL_GAP = 1;

    final Texture texture;
    final Char2ObjectOpenHashMap<Glyph> glyphTable;
    final Glyph unknownGlyph;
    final float ascent, descent, leading;

    Font(Texture texture, Char2ObjectOpenHashMap<Glyph> glyphTable, Glyph unknownGlyph,
         float ascent, float descent, float leading) {
        this.texture = texture;
        this.glyphTable = glyphTable;
        this.unknownGlyph = unknownGlyph;
        this.ascent = ascent;
        this.descent = descent;
        this.leading = leading;
    }

    public Glyph getGlyph(char ch) {
        return glyphTable.get(ch);
    }

    public Texture texture() {
        return texture;
    }

    public Glyph unknownGlyph() { return unknownGlyph; }

    /// Вынос наверх. По сути максимальная величина высоты из глифов
    public float ascent()  { return ascent; }
    /// Вынос вниз. Для букв по типу 'y'
    public float descent() { return descent; }
    /// Межстрочный интервал. Позволяет избежать слипание текста
    public float leading() { return leading; }

    public float lineHeight() { return descent + ascent + leading; }

    public static final class Glyph implements Drawable {
        private final short fontTexId;
        private final byte width, height;
        private final short u, v, u2, v2;

        public Glyph(Texture fontTex, byte width, byte height, short x, short y) {
            this.fontTexId = fontTex.id;
            this.width = width;
            this.height = height;

            this.u  = BytePack.toB16((x + 0.f) / fontTex.width());
            this.v  = BytePack.toB16((y + 0.f) / fontTex.height());
            this.u2 = BytePack.toB16((1f * x + width) / fontTex.width());
            this.v2 = BytePack.toB16((1f * y + height) / fontTex.height());
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
