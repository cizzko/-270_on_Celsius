package core.g2d;

import core.assets.AssetHandler;
import core.assets.AssetReleaser;
import core.assets.AssetResolver;
import core.graphic.RectanglePacker;
import core.math.MathUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import static core.g2d.Font.PIXEL_GAP;
import static core.g2d.Font.fontSize;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public final class FontHandler extends AssetHandler<Font, Void, FontHandler.State> {

    public FontHandler() {
        super(Font.class, "fonts");
    }

    @Override
    public void release(AssetReleaser rel, Font asset) {
        rel.release(asset.texture);
    }

    @Override
    public void loadAsync(AssetResolver res, String name, Void params, State state) {
        state.texture = res.fork(() -> {

            java.awt.Font awtFont;
            try (var in = Files.newInputStream(dir.resolve(name))) {
                awtFont = java.awt.Font.createFont(java.awt.Font.PLAIN, in);
            }
            // default 12
            awtFont = awtFont.deriveFont(java.awt.Font.PLAIN, (float) (fontSize * Toolkit.getDefaultToolkit().getScreenResolution() / 72.0));

            Font fnt = new Font();
            HashMap<Character, Font.Glyph> glyphTableTmp = new HashMap<>();

            BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D tmpg = tmp.createGraphics();
            {
                tmpg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                tmpg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                tmpg.setFont(awtFont);
                tmpg.dispose();
            }
            FontMetrics metrics = tmpg.getFontMetrics();

            record GlyphAndImage(Font.Glyph glyph, BufferedImage image) {}

            // TODO:
            // Мне не понравилось работать с java.awt.Font
            // Совершенно нет идей как делать отрисовку глифов других параметров (italic, bold, другой размер и т.д.)
            // Я видел бинды на FreeType на манер LWJGL, может их и использовать?
            // P.S. Тут в коде очень опасная ситуация может быть. Дело в размере текстуры.
            // При превышении этого значения пойдут артефакты. Это можно решить разбив шрифт на "страницы",
            // но надо это ещё надо подумать...

            RectanglePacker packer = new RectanglePacker(64, 64);

            ArrayList<GlyphAndImage> glyphs = new ArrayList<>();

            int guessedHeight = metrics.getHeight();
            for (char c = Character.MIN_VALUE; c < Character.MAX_VALUE; c++) {
                if (!awtFont.canDisplay(c)) {
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
                g2.setFont(awtFont);
                g2.setColor(Color.WHITE);
                g2.drawString(Character.toString(c), 0, metrics.getAscent());
                g2.dispose();

                int width = image.getWidth();
                int height = image.getHeight();
                Font.Glyph ch = new Font.Glyph(fnt, c, width, height);

                glyphs.add(new GlyphAndImage(ch, image));
                glyphTableTmp.put(c, ch);
            }

            for (GlyphAndImage image : glyphs) {
                Font.Glyph gl = image.glyph;

                RectanglePacker.Position pos;
                while ((pos = packer.pack(gl.width(), gl.height(), PIXEL_GAP)).isInvalid()) {
                    boolean increaseW = packer.w <= packer.h;
                    // if (packer.w >= maxTexSize && increaseW) {
                    //     throw new IllegalArgumentException();
                    // }
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
                Font.Glyph gl = p.glyph;
                gr.drawImage(p.image, gl.x, gl.y, null);
            }
            gr.dispose();
            // Копирование необходимо, чтобы ужать хеш-таблицу до оптимального размера
            return new FontData(fnt, atlasImage, Map.copyOf(glyphTableTmp));
        });
    }

    @Override
    public Font loadSync(String name, Void params, State state) {
        var glyphData = state.texture.resultNow();

        var fnt = glyphData.fnt;
        fnt.texture = Texture.load(glyphData.atlas, GL_TEXTURE_2D, 0, 0, 1, 1);
        fnt.glyphTable = glyphData.glyphTable;
        fnt.unknownGlyph = fnt.glyphTable.get('?');

        for (Font.Glyph glyph : glyphData.glyphTable.values()) {
            glyph.computeTextureCoordinates();
        }
        return fnt;
    }

    @Override
    protected Void createParams() {
        return null;
    }

    @Override
    protected State createState() {
        return new State();
    }

    public static final class State {
        private Future<FontData> texture;
    }

    public record FontData(Font fnt, BufferedImage atlas, Map<Character, Font.Glyph> glyphTable) {

    }
}
