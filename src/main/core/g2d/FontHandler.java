package core.g2d;

import core.assets.AssetHandler;
import core.assets.AssetReleaser;
import core.assets.AssetResolver;
import core.graphic.RectanglePacker;
import core.math.MathUtil;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.Future;

import static core.g2d.Font.PIXEL_GAP;
import static core.g2d.Font.fontSize;
import static core.math.MathUtil.toByteExact;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

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
            awtFont = awtFont.deriveFont(java.awt.Font.PLAIN, (float) (fontSize * Toolkit.getDefaultToolkit().getScreenResolution() / 72.0));

            Font fnt = new Font();

            BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D tmpg = tmp.createGraphics();
            {
                tmpg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                tmpg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                tmpg.setFont(awtFont);
                tmpg.dispose();
            }
            FontMetrics metrics = tmpg.getFontMetrics();

            // TODO:
            // Мне не понравилось работать с java.awt.Font
            // Совершенно нет идей как делать отрисовку глифов других параметров (italic, bold, другой размер и т.д.)
            // Я видел бинды на FreeType на манер LWJGL, может их и использовать?
            // P.S. Тут в коде очень опасная ситуация может быть. Дело в размере текстуры.
            // При превышении этого значения пойдут артефакты. Это можно решить разбив шрифт на "страницы",
            // но надо это ещё надо подумать...

            RectanglePacker packer = new RectanglePacker(64, 64);

            ArrayList<GlyphPacked> glyphs = new ArrayList<>();

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

                glyphs.add(new GlyphPacked(c, image));
            }

            for (var packed : glyphs) {

                RectanglePacker.Position pos;
                while ((pos = packer.pack(packed.w, packed.h, PIXEL_GAP)).isInvalid()) {
                    if (packer.w <= packer.h) {
                        packer.resize(MathUtil.ceilNextPowerOfTwo(packer.w + 1), packer.h);
                    } else {
                        packer.resize(packer.w, MathUtil.ceilNextPowerOfTwo(packer.h + 1));
                    }
                }
                packed.x = MathUtil.toShortExact(pos.x);
                packed.y = MathUtil.toShortExact(pos.y);
            }

            BufferedImage atlasImage = new BufferedImage(packer.w, packer.h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gr = atlasImage.createGraphics();
            for (var p : glyphs) {
                gr.drawImage(p.image, p.x, p.y, null);
            }
            gr.dispose();

            return new FontData(fnt, atlasImage, glyphs);
        });
    }

    static final class GlyphPacked {
        BufferedImage image;
        short x, y;
        byte w, h;
        char ch;

        GlyphPacked(char ch, BufferedImage image) {
            this.ch = ch;
            this.image = image;
            this.w = toByteExact(image.getWidth());
            this.h = toByteExact(image.getHeight());
        }
    }

    @Override
    public Font loadSync(AssetResolver res, String name, Void params, State state) {
        var glyphData = res.join(state.texture);
        res.checkIfFailed();

        var fnt = glyphData.fnt;
        fnt.texture = Texture.load(glyphData.atlas, GL_TEXTURE_2D, GL_CLAMP_TO_EDGE, (short) 0, (short) 0, (short) 1, (short) 1);

        var glyphs = glyphData.glyphs;
        var glyphTable = new Char2ObjectOpenHashMap<Font.Glyph>(glyphs.size());
        for (GlyphPacked glyph : glyphs) {
            glyphTable.put(glyph.ch, new Font.Glyph(fnt, glyph.w, glyph.h, glyph.x, glyph.y));
        }
        glyphTable.trim();

        fnt.glyphTable = glyphTable;
        Font.Glyph unknownGlyph = glyphTable.get('?');
        glyphTable.defaultReturnValue(unknownGlyph);
        fnt.unknownGlyph = unknownGlyph;
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

    public record FontData(Font fnt, BufferedImage atlas, ArrayList<GlyphPacked> glyphs) {

    }
}
