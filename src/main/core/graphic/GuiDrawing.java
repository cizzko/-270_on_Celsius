package core.graphic;

import core.UI.Styles;
import core.Window;
import core.content.ItemStack;
import core.content.items.Item;
import core.g2d.*;
import core.math.Point2i;

import static core.Global.*;
import static core.WorldCoordinates.*;
import static core.g2d.StackfulRender.*;
import static core.graphic.WorldDrawing.viewport;

/// Примитивы рендера в интерфейсе игры
public class GuiDrawing {
    // Размер блока в пикселях (базовый),
    // при изменении от него отталкивается масштабирование мира
    public static final int blockSize  = 48;
    // Размер предмета в мире, интерфейсе
    public static final float itemSize = 32;

    private static final Point2i textSize = new Point2i();

    public static void drawObjects(float x, float y, ItemStack[] items, Atlas.Region iconRegion) {
        if (items.length == 0) {
            return;
        }
        draw(iconRegion, x, y + 16);

        for (int i = 0; i < items.length; i++) {
            var item = items[i];
            if (item == null) {
                continue;
            }

            int playerSize = Math.max(player.creature.texture.width(), player.creature.texture.height());
            drawText((x + (i * 54)) + playerSize + 28, y + 3,
                    item.count() > 9 ? "9+" : String.valueOf(item.count()), Styles.DIRTY_BRIGHT_BLACK);

            float uiScale = item.item().uiScale();
            var tex = item.item().texture;
            draw(tex, (x + (i * 54)) + playerSize + 5, y + 15,
                    tex.width() * uiScale, tex.height() * uiScale);
        }
    }

    public static void drawText(float x, float y, CharSequence text, Color color) {
        drawText(x, y, text, color.rgba8888());
    }

    public static void drawText(float x, float y, CharSequence text, int rgba8888) {
        float startX = x;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == '\\' && i + 1 < text.length() && text.charAt(i + 1) == 'n') {
                y -= 30;
                i++;
                x = startX;
                continue;
            }
            Font.Glyph glyph = Window.defaultFont.getGlyph(ch);
            draw(glyph, rgba8888, x, y);
            x += glyph.width();
        }
    }

    public static void drawText(float x, float y, CharSequence text) {
        drawText(x, y, text, Styles.TEXT_COLOR);
    }

    public static Point2i calculateTextSize(String text) {
        String longestLine = "";
        int width = 12;
        int linesCount = 0;

        for (String line : text.split("\\\\n")) {
            linesCount++;
            if (line.length() >= longestLine.replaceAll("\\s+", "").length()) {
                longestLine = line;
            }
        }

        for (int i = 0; i < longestLine.length(); i++) {
            char c = longestLine.charAt(i);
            width += Window.defaultFont.getGlyph(c).width();
        }
        return textSize.set(width, linesCount * 28 + 16);
    }

    public static void drawItemStack(float x, float y, ItemStack item) {
        drawItem(x, y, item.item());
        drawText(x + 28, y - 7, item.count() > 9 ? "9+" : String.valueOf(item.count()), Styles.DIRTY_BRIGHT_BLACK);
    }

    public static void drawItem(float x, float y, Item item) {
        float uiScale = item.uiScale();
        Atlas.Region tex = item.texture;
        draw(tex, x + 5, y + 5, tex.width() * uiScale, tex.width() * uiScale);
    }

    public static void drawBlocksGui() {
        StackfulRender.z(Render.LAYER_GUI);
        StackfulRender.camera(uiScene.view());
        camera.getBoundsTo(viewport);
        int minX = Math.max(0, toBlock(viewport.x));
        int minY = Math.max(0, toBlock(viewport.y));
        int maxX = Math.min(world.sizeX - 1, toBlock(viewport.x + viewport.width));
        int maxY = Math.min(world.sizeY - 1, toBlock(viewport.y + viewport.height));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                var entity = world.getEntity(x, y);
                if (entity != null) {
                    entity.drawGui();
                }
            }
        }
    }
}
