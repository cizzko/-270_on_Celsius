package core.World.Textures;

import core.UI.Styles;
import core.Window;
import core.World.Creatures.DynamicWorldObjects;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.Creatures.Player.Inventory.Items.Weapons.Ammo.Bullets;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.Structures.Chests;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.StaticWorldObjects.TileData;
import core.g2d.Atlas;
import core.g2d.Fill;
import core.g2d.Font;
import core.math.Point2i;
import core.math.Rectangle;
import core.util.Color;

import java.util.ArrayDeque;

import static core.Global.*;
import static core.World.Creatures.Player.Player.playerSize;
import static core.World.WorldGenerator.WorldGenerator.*;

public class TextureDrawing {
    //todo разобраться с размерами
    public static final int blockSize = 48;

    private static final ArrayDeque<BlockPreview> previewBlocks = new ArrayDeque<>();

    private static final Rectangle viewport = new Rectangle();

    //todo переместить
    public static void drawObjects(float x, float y, ItemStack[] items, Atlas.Region iconRegion) {
        if (items.length == 0) {
            return;
        }
        batch.draw(iconRegion, x, y + 16);

        for (int i = 0; i < items.length; i++) {
            var item = items[i];
            float scale = item.getItem().getUiScale();

            drawText((x + (i * 54)) + playerSize + 28, y + 3,
                    item.getCount() > 9 ? "9+" : String.valueOf(item.getCount()), Styles.DIRTY_BRIGHT_BLACK);

            int finalI = i;
            batch.pushState(() -> {
                batch.scale(scale);
                batch.draw(item.getItem().texture, ((x + (finalI * 54)) + playerSize + 5), (y + 15));
            });
        }
    }

    public record BlockPreview(int x, int y, short blockId, byte hp, boolean breakable) {}

    public static void drawText(float x, float y, String text, Color color) {
        float startX = x;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == ' ') {
                x += Window.defaultFont.getGlyph('A').width();
                continue;
            } else if (ch == '\\' && i + 1 < text.length() && text.charAt(i + 1) == 'n') {
                y -= 30;
                i++;
                x = startX;
                continue;
            }
            Font.Glyph glyph = Window.defaultFont.getGlyph(ch);
            batch.draw(glyph, color, x, y);
            x += glyph.width();
        }
    }

    public static void drawText(float x, float y, String text) {
        drawText(x, y, text, Styles.TEXT_COLOR);
    }

    public static void drawPointArray(Point2i[] points) {
        Fill.lineWidth(4f);

        var color = Color.fromRgba8888(0, 0, 0, 1);
        float d = blockSize + 8;

        for (int i = 0; i < points.length - 1; i++) {
            Fill.line(points[i].x * d, points[i].y * d, points[i + 1].x * d, points[i + 1].y * d, color);
        }
        Fill.resetLineWidth();
    }

    public static void drawRectangleText(int x, int y, int maxWidth, String text, boolean staticTransfer, Color panColor, Font font) {
        maxWidth = (maxWidth > 0 ? maxWidth : 1920 - x);
        y = staticTransfer ? y + getTextSize(text).x / maxWidth * blockSize : y;

        StringBuilder modifiedText = new StringBuilder();
        int currentWidth = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == ' ') {
                currentWidth += font.getGlyph('A').width();
            } else {
                currentWidth += font.getGlyph(c).width();
            }
            if (currentWidth > maxWidth) {
                modifiedText.append("\\n");
                currentWidth = 0;
            }
            modifiedText.append(c);
        }
        text = modifiedText.toString();

        var textSize = getTextSize(text);
        int width = textSize.x;
        int height = textSize.y;

        Fill.rect(x + 30, y - height / 2f, width, height, panColor);
        drawText(x + 36, y + height - 32 - height / 2f, text);
    }

    private static final Point2i textSize = new Point2i();

    public static Point2i getTextSize(String text) {
        String longestLine = "";
        int width = 12;
        int linesCount = 0;

        // find '\\n'
        for (String line : text.split("\\\\n")) {
            linesCount++;
            if (line.length() >= longestLine.replaceAll("\\s+", "").length()) {
                longestLine = line;
            }
        }

        for (int i = 0; i < longestLine.length(); i++) {
            char c = longestLine.charAt(i);

            if (c == ' ') {
                width += Window.defaultFont.getGlyph('A').width();
                continue;
            }
            width += Window.defaultFont.getGlyph(c).width();
        }
        return textSize.set(width, linesCount * 28 + 16);
    }

    public static void drawStatic() {
        camera.getBoundsTo(viewport);
        int minX = (int) Math.floor(viewport.x / blockSize);
        int maxX = (int) Math.floor((viewport.x + viewport.width) / blockSize);
        int minY = (int) Math.floor(viewport.y / blockSize);
        int maxY = (int) Math.floor((viewport.y + viewport.height) / blockSize);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (!world.inBounds(x, y)) {
                    continue;
                }
                var obj = world.getBlock(x, y);
                if (obj == null || obj == StaticObjectsConst.AIR || obj.texture == atlas.getErrorRegion()) {
                    continue;
                }
                int hp = world.getHp(x, y);
                // todo расширить проверку для структур, слева пропадают раньше чем должны
                drawBlock(x, y, obj, hp);
            }
        }

        //todo вынести куда то
        Chests.draw();

        for (BlockPreview q : previewBlocks) {
            drawQueuedBlock(q.x, q.y, q.blockId, q.hp, q.breakable);
        }
        previewBlocks.clear();
    }

    private static final Color tmp = new Color();

    private static void drawQueuedBlock(int x, int y, short blockId, byte hp, boolean breakable) {
        var block = content.getConstByBlockId(blockId);
        if (block == null || block.texture == atlas.getErrorRegion()) {
            return;
        }

        int wx = x * blockSize;
        int wy = y * blockSize;

        if (isOnCamera(wx, wy, block.texture)) {
            Color color = ShadowMap.getColorTo(x, y, tmp);
            int a = (color.r() + color.g() + color.b()) / 3;
            if (breakable) {
                color.set(Math.max(0, a - 150), Math.max(0, a - 150), a, 255);
            } else {
                color.set(a, Math.max(0, a - 150), Math.max(0, a - 150), 255);
            }

            batch.draw(block.texture, color, wx, wy);
            drawDamage(block, hp, wx, wy);
        }
    }

    private static void drawBlock(int x, int y, StaticObjectsConst obj, int hp) {
        int xBlock = findX(x, y);
        int yBlock = findY(x, y);

        if (world.getData(x, y) instanceof TileData.MultiblockPart) {
            drawDamage(obj, hp, xBlock, yBlock);
            return;
        }

        Color color = ShadowMap.getColorTo(x, y, tmp);
        int upperLimit = 100;
        int lowestLimit = -20;
        int maxColor = 65;
        float temp = TemperatureMap.getTemp(x, y);

        int a;
        if (temp > upperLimit) {
            a = (int) Math.min(maxColor, Math.abs((temp - upperLimit) / 3));
            color.set(color.r(), color.g() - (a / 2), color.b() - a, color.a());
        } else if (temp < lowestLimit) {
            a = (int) Math.min(maxColor, Math.abs((temp + lowestLimit) / 3));
            color.set(color.r() - a, color.g() - (a / 2), color.b(), color.a());
        }

        batch.draw(obj.texture, color, xBlock, yBlock);
        drawDamage(obj, hp, xBlock, yBlock);

        var blockEntity = world.getEntity(x, y);
        if (blockEntity != null) {
            blockEntity.draw();
        }
    }

    private static void drawDamage(StaticObjectsConst obj, int hp, int xBlock, int yBlock) {
        if (hp > obj.maxHp / 1.5f) {
            // ???
        } else if (hp < obj.maxHp / 3) {
            batch.draw(atlas.byPath("World/Blocks/damaged1"), xBlock, yBlock);
        } else {
            batch.draw(atlas.byPath("World/Blocks/damaged0"), xBlock, yBlock);
        }
    }

    public static void addBlockPreview(int blockX, int blockY, short blockId, byte hp, boolean breakable) {
        previewBlocks.add(new BlockPreview(blockX, blockY, blockId, hp, breakable));
    }

    public static boolean isOnCamera(float x, float y, Atlas.Region texture) {
        camera.getBoundsTo(viewport);

        return viewport.contains(x, y, texture.width(), texture.height());
    }

    public static void drawDynamic() {
        for (DynamicWorldObjects dynamicObject : DynamicObjects) {
            if (dynamicObject != null) {
                dynamicObject.incrementCurrentFrame();

                if (isOnCamera(dynamicObject.getX(), dynamicObject.getY(), dynamicObject.getTexture())) {
                    if (dynamicObject.getFramesCount() == 0) {
                        var shadow = ShadowMap.getColorDynamic(dynamicObject);
                        batch.draw(dynamicObject.getTexture()/*, shadow*/, dynamicObject.getX(), dynamicObject.getY());
                    } else {
                        // todo дописать
                        // drawTexture(dynamicObject.getPath() + dynamicObject.getCurrentFrame() + ".png", dynamicObject.getX(), dynamicObject.getY(), ShadowMap.getColorDynamic(), false, false);
                    }
                }
            }
        }

        //todo а может сделать пули как сущности? чтоб ничего не считать отдельно
        Bullets.drawBullets();
    }
}
