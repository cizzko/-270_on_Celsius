package core.UI;

import core.g2d.Font;
import core.math.Rectangle;
import core.pool.Poolable;
import core.util.Color;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.HexFormat;

public final class GlyphCache {
    // private static final Pool<GlyphData> GLYPHS = new Pool<>(GlyphData::new, 1024);

    private final ObjectArrayList<GlyphData> glyphs = new ObjectArrayList<>();

    private int count;
    private boolean parseMarkup;
    public final Rectangle rect = new Rectangle();

    public void setParseMarkup(boolean parseMarkup) {
        this.parseMarkup = parseMarkup;
    }

    public void setText(Font font,
                        CharSequence text, int begin, int length,
                        Color color,
                        float x, float y) {
        resize(length);

        IntArrayFIFOQueue colorDeque = new IntArrayFIFOQueue();
        colorDeque.enqueue(color.rgba8888());
        float lineWidth = x;
        float lineHeight = y;
        float maxGliphHeight = 0;

        float maxX = 0;

        int displayeableChars = 0;
        for (int i = 0; i < length; i++) {
            // char c = text.charAt(begin + i);
            // if (parseMarkup && c == '{') {
            //     int p = tryParseMarkup(text, i + begin + 1, begin + length, colorDeque);
            //     if (p > 0) {
            //         i += p;
            //     } else if (p == -2) {
            //         // escaped {{
            //         i++;
            //     } else { // p == -1
            //         // ordinal character
            //     }
            // }
            // if (i >= begin+length) {
            //     break;
            // }

            char c = text.charAt(begin + i);
            if (c == '\n') {
                lineWidth = x;
                lineHeight -= 30; //междустрочный интервал
            } else {
                var gl = font.getGlyph(c);
                var data = glyphs.get(displayeableChars);
                data.x = lineWidth;
                data.y = lineHeight;
                data.rgba8888 = colorDeque.lastInt();
                data.glyph = gl;
                lineWidth += gl.width();
                maxX = Math.max(maxX, lineWidth);
                maxGliphHeight = Math.max(maxGliphHeight, gl.height());
                displayeableChars++;
            }
        }

        // xy ➡️ ⬇️
        float minX = x;
        float minY = lineHeight;
        // System.out.println("maxGliphHeight = " + maxGliphHeight + " | y= " + y);
        float maxY = y + maxGliphHeight;

        rect.set(minX, minY, maxX - minX, maxY - minY);

        count = displayeableChars;
    }

    private int tryParseMarkup(CharSequence str, int start, int end,
                               IntArrayFIFOQueue colorDeque) {
        if (start == end) {
            return -1;
        }

        char n = str.charAt(start);
        switch (n) {
            case '{' -> {
                // {{
                return -2;
            }
            case '#' -> {
                // {#RRGGBB} или {#RRGGBBAA}
                // То есть минимум надо 7 символов, но поскольку цвет парсится для символов, то
                // 8, чтобы это не выглядело бесполезно
                int colorEnd = start + 2;
                if (colorEnd < end) {
                    int colorBegin = start + 1;
                    // попытаемся прочитать в полном формате
                    colorEnd = Math.min(start + 10, end);
                    for (int i = colorBegin; i < colorEnd; i++) {
                        char ch = str.charAt(i);
                        if (HexFormat.isHexDigit(ch)) {
                            continue;
                        } else if (ch == '}') {
                            int color = HexFormat.fromHexDigits(str, colorBegin, i);
                            if (i - start <= 7) {
                                int k = 8 - (i - colorBegin);
                                color <<= 4 * k;
                                color |= 0xff;
                            }
                            colorDeque.enqueue(color);
                            return 2 + i - start;
                        } else {
                            break;
                        }
                    }
                }
            }
            case '}' -> {
                // {}
                if (colorDeque.size() > 1) {
                    colorDeque.dequeueInt();
                }
                return 2;
            }
        }
        return -1;
    }

    public void recomputePosition(float x, float y) {
        // TODO всего лишь параллельный перенос, меняем од
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
        // width = gx - x;
        // height = gheight;
    }

    public int getCount() {
        return count;
    }

    public ObjectArrayList<GlyphData> getGlyphs() {
        return glyphs;
    }


    public void reset() {
        rect.set(0,0,0,0);
        count = 0;
        // for (GlyphData glyph : glyphs) {
        //     GLYPHS.free(glyph);
        // }
        glyphs.clear();
    }

    private void resize(int len) {
        glyphs.ensureCapacity(len);
        int toAdd = len - glyphs.size();
        for (GlyphData glyph : glyphs) {
            // GLYPHS.free(glyph);
            glyph.reset();
        }
        if (toAdd > 0) {
            for (int i = 0; i < toAdd; i++) {
                // glyphs.add(GLYPHS.obtain());
                glyphs.add(new GlyphData());
            }
        }
    }

    public static final class GlyphData implements Poolable {
        public float x, y;
        public int rgba8888;
        public Font.Glyph glyph;

        @Override
        public void reset() {
            x = y = 0;
            rgba8888 = 0;
            glyph = null;
        }

        @Override
        public String toString() {
            return "GlyphData{" +
                   "x=" + x +
                   ", y=" + y +
                   ", rgba8888=" + rgba8888 +
                   ", glyph=" + glyph +
                   '}';
        }
    }
}
