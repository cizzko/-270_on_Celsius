package core.g2d;

import core.EventHandling.Logging.Logger;
import core.graphic.RectanglePacker;
import core.math.MathUtil;
import org.lwjgl.opengl.GL46;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public final class Font {
    private static final int fontSize = 18;

    private Texture texture;
    private Map<Character, Glyph> glyphTable;
    private Glyph unknownGlyph;

    public static Font load(String pathTTF) throws IOException {
        java.awt.Font font = null;
        try {
            font = java.awt.Font.createFont(java.awt.Font.PLAIN, new File(pathTTF));
            // default 12
            font = font.deriveFont(java.awt.Font.PLAIN, (float) (fontSize * Toolkit.getDefaultToolkit().getScreenResolution() / 72.0));
        } catch (IOException | FontFormatException e) {
            Logger.printException("Error when generate font", e);
            Logger.logExit(1);
        }

        Font fnt = new Font();
        HashMap<Character, Glyph> glyphTableTmp = new HashMap<>();

        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tmpg = tmp.createGraphics();
        {
            tmpg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            tmpg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            tmpg.setFont(font);
            tmpg.dispose();
        }
        FontMetrics metrics = tmpg.getFontMetrics();

        record GlyphAndImage(Glyph glyph, BufferedImage image) {}

        // TODO:
        // Мне не понравилось работать с java.awt.Font
        // Совершенно нет идей как делать отрисовку глифов других параметров (italic, bold, другой размер и т.д.)
        // Я видел бинды на FreeType на манер LWJGL, может их и использовать?
        // P.S. Тут в коде очень опасная ситуация может быть. Дело в размере текстуры.
        // При превышении этого значения пойдут артефакты. Это можно решить разбив шрифт на "страницы",
        // но надо это ещё надо подумать...

        int maxTexSize = GL46.glGetInteger(GL46.GL_MAX_TEXTURE_SIZE);
        RectanglePacker packer = new RectanglePacker(64, 64);

        ArrayList<GlyphAndImage> glyphs = new ArrayList<>();

        int guessedHeight = metrics.getHeight();
        for (char c = Character.MIN_VALUE; c < Character.MAX_VALUE; c++) {
            if (!font.canDisplay(c)) {
                continue;
            }
            int charWidth = metrics.charWidth(c);
            if (charWidth == 0) {
                continue;
            }

            BufferedImage image = new BufferedImage(charWidth, guessedHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setFont(font);
            g2.setColor(java.awt.Color.WHITE);
            g2.drawString(Character.toString(c), 0, metrics.getAscent());
            g2.dispose();

            int width = image.getWidth();
            int height = image.getHeight();
            Glyph ch = new Glyph(fnt, c, width, height);

            glyphs.add(new GlyphAndImage(ch, image));
            glyphTableTmp.put(c, ch);
        }

        for (GlyphAndImage image : glyphs) {
            Glyph gl = image.glyph;

            RectanglePacker.Position pos;
            while ((pos = packer.pack(gl.width, gl.height)).isInvalid()) {
                boolean increaseW = packer.w <= packer.h;
                if (packer.w >= maxTexSize && increaseW) {
                    throw new IllegalArgumentException();
                }
                if (increaseW) {
                    packer.resize(MathUtil.ceilNextPowerOfTwo(packer.w + 1), packer.h);
                } else {
                    packer.resize(packer.w, MathUtil.ceilNextPowerOfTwo(packer.h + 1));
                }
            }
            gl.x = pos.x;
            gl.y = pos.y;
        }

        BufferedImage atlasImage = new BufferedImage(packer.w, packer.h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = atlasImage.createGraphics();
        for (GlyphAndImage p : glyphs) {
            Glyph gl = p.glyph;
            gr.drawImage(p.image, gl.x, gl.y, null);
        }
        gr.dispose();

        fnt.texture = Texture.load(atlasImage, GL_TEXTURE_2D, 0, 0, 1, 1);
        // Копирование необходимо, чтобы ужать хеш-таблицу до оптимального размера
        fnt.glyphTable = Map.copyOf(glyphTableTmp);
        fnt.unknownGlyph = fnt.glyphTable.get('?');

        for (GlyphAndImage glyph : glyphs) {
            glyph.glyph.computeTextureCoordinates();
        }
        return fnt;
    }

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

        private int x, y;
        private float u, v, u2, v2;

        public Glyph(Font font, char ch,
                     int width, int height) {
            this.font = font;
            this.ch = ch;
            this.width = width;
            this.height = height;
        }

        private void computeTextureCoordinates() {
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
    }
}
