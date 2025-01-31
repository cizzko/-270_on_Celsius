package core.ui;

import core.util.Color;
import core.g2d.Font;

import java.util.ArrayList;

public final class GlyphCache {
    private final ArrayList<GlyphData> glyphs = new ArrayList<>();

    private float width, height;
    private int count;

    public void setText(Font font,
                        CharSequence text, int begin, int length,
                        Color color,
                        float x, float y) {
        this.count = length;
        resize(length);
        float gx = x;
        float gheight = 0;
        for (int i = 0; i < length; i++) {
            char c = text.charAt(begin + i);
            var gl = font.getGlyph(c);
            var data = glyphs.get(i);
            data.x = gx;
            data.y = y;
            data.colorRgba = color.rgba8888();
            data.glyph = gl;
            gx += gl.width();
            gheight = Math.max(gheight, gl.height());
        }
        width = gx - x;
        height = gheight;
    }

    public void recomputePosition(float x, float y) {
        float gx = x;
        float gheight = 0;
        int c = count;
        for (int i = 0; i < c; i++) {
            var data = glyphs.get(i);
            data.x = gx;
            data.y = y;
            gx += data.glyph.width();
            gheight = Math.max(gheight, data.glyph.height());
        }
        width = gx - x;
        height = gheight;
    }

    public int getCount() {
        return count;
    }

    public ArrayList<GlyphData> getGlyphs() {
        return glyphs;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
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

    public static final class GlyphData {
        public float x, y;
        public int colorRgba;
        public Font.Glyph glyph;

        void reset() {
            x = y = 0;
            colorRgba = 0;
            glyph = null;
        }
    }
}
