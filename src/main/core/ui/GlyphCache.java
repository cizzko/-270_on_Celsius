package core.ui;

import core.g2d.Font;
import core.graphic.Color;
import core.pool.Poolable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class GlyphCache {
    private final ObjectArrayList<GlyphData> glyphs = new ObjectArrayList<>();

    private static final int MODE_BY_WORDS     = 1;
    private static final int MODE_BY_DELIMITER = 2;

    private float width, height;
    // private float maxWidth;
    // private int maxSize = Integer.MAX_VALUE;
    // private String tail, delimiter;
    // private int mode = MODE_BY_WORDS;
    private int count;

    // [begin,begin+length)
    public void setText(Font font, CharSequence text, int begin, int length,
                        Color color) {
        resize(length);

        float lineWidth = 0, lineHeight = 0;
        int displayableChars = 0;

        int rgba8888 = color.rgba8888();

        for (int end = begin + length; begin < end; begin++) {
            char c = text.charAt(begin);
            var gl = font.getGlyph(c);

            var data = glyphs.get(displayableChars);
            data.offsetX = lineWidth;
            data.offsetY = lineHeight;
            data.rgba8888 = rgba8888;
            data.glyph = gl;

            lineWidth += gl.width();
            displayableChars++;
        }

        width = lineWidth;
        height = font.lineHeight();
        count = displayableChars;
    }

    public float width() { return width; }
    public float height() { return height; }
    public int count() { return count; }
    public ObjectArrayList<GlyphData> glyphs() { return glyphs; }

    public void reset() {
        width = height = 0;
        count = 0;
        glyphs.clear();
    }

    private void resize(int len) {
        glyphs.ensureCapacity(len);
        int toAdd = len - glyphs.size();
        for (GlyphData glyph : glyphs) {
            glyph.reset();
        }
        if (toAdd > 0) {
            for (int i = 0; i < toAdd; i++) {
                glyphs.add(new GlyphData());
            }
        }
    }

    public static final class GlyphData implements Poolable {
        public float offsetX, offsetY;
        public int rgba8888;
        public Font.Glyph glyph;

        public void reset() {
            offsetX = offsetY = 0;
            rgba8888 = 0;
            glyph = null;
        }
    }
}
