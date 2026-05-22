package core.World.Textures;

import core.Constants;
import core.Global;
import core.UI.Styles;
import core.Window;
import core.World.Creatures.Physics;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.TemperatureMap;
import core.content.ItemStack;
import core.content.blocks.data.TileData;
import core.content.entity.DrawComponent;
import core.content.items.Item;
import core.g2d.*;
import core.math.Point2i;
import core.math.Rectangle;
import core.util.Color;
import core.util.FixedBitset;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;

import static core.Constants.World.SWAP_AREA;
import static core.Global.*;
import static core.World.Creatures.Player.Inventory.Inventory.drawBuildGrid;
import static core.g2d.StackfulRender.*;
import static core.util.FixedBitset.*;

public class TextureDrawing {
    // Размер блока в пикселях (базовый),
    // при изменении от него отталкивается масштабирование мира
    public static final int blockSize  = 48;
    // Размер предмета в мире, интерфейсе
    public static final float itemSize = 32;

    public static int toBlock(float worldPos) {
        return (int) Math.floor(worldPos / blockSize);
    }

    public static float toWorld(int blockPos) {
        return blockPos * blockSize;
    }

    public static void resetState() {
        previewBlocks.clear();
    }

    private record BlockPreview(int x, int y, short blockId, byte hp, boolean canBreak) {
    }

    private static final ArrayList<BlockPreview> previewBlocks = new ArrayList<>();

    public static final Rectangle viewport = new Rectangle(); // TODO убрать в Camera2

    private static final Rectangle hitbox = new Rectangle();
    private static final Point2i textSize = new Point2i();
    private static final Color tmp = new Color();

    public static void drawObjects(float x, float y, ItemStack[] items, Atlas.Region iconRegion) {
        if (items.length == 0) {
            return;
        }
        draw(iconRegion, x, y + 16);

        for (int i = 0; i < items.length; i++) {
            var item = items[i];

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

    public static void drawPointArray(Point2i[] points) {
        Fill.lineWidth(4f);

        var color = Color.fromRgba8888(0, 0, 0, 1);
        float d = blockSize + 8;

        for (int i = 0; i < points.length - 1; i++) {
            Fill.line(points[i].x * d, points[i].y * d, points[i + 1].x * d, points[i + 1].y * d, color);
        }
        Fill.resetLineWidth();
    }

    public static Point2i calculateTextSize(String text) {
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
            width += Window.defaultFont.getGlyph(c).width();
        }
        return textSize.set(width, linesCount * 28 + 16);
    }

    private static class Chunk {
        private final RenderList renderList = Render.queue().allocRList(RenderList.KIND_STATIC);

        private boolean drawStateChanged;
        private int lastPreviewBlocks;
        private final Rectangle lastBounds = new Rectangle();
        private final Point2i pos = new Point2i();

        static final int MARGIN = 2;

        record MergedTile(short blockId, int shadowRgba8888, int x, int y, short w, short h) {}

        int rows, cols;
        int minX, minY;
        int maxX, maxY;
        long[] processed;
        long[] merged;

        final Short2ObjectOpenHashMap<ArrayList<MergedTile>> rects = new Short2ObjectOpenHashMap<ArrayList<MergedTile>>();

        int pos2index(int x, int y) {
            return (x - minX) + (maxX - minX + 1) * (y - minY);
        }

        boolean isProcessed(int x, int y) {
            return isSet(processed, pos2index(x, y));
        }

        boolean isSame(int x1, int y1, int x2, int y2) {
            return !isProcessed(x2, y2) &&
                   world.getBlockId(x1, y1) == world.getBlockId(x2, y2)
                   && isSameShadowAndTemp(x1, y1, x2, y2)
                    ;
        }

        int maxY2, maxX2, maxArea;

        private void findMaxRectangleFrom(int bx, int by) {
            maxArea = 1;
            maxY2 = by;
            maxX2 = bx;

            int cmx = bx;
            while (cmx <= maxX && isSame(bx, by, cmx, by)) cmx++;
            cmx--;

            for (int y = by; y <= maxY; y++) {
                if (!isSame(bx, by, bx, y)) break;

                int x = bx;
                while (x <= cmx && isSame(bx, by, x, y)) x++;
                int validWidth = x - bx;
                if (validWidth == 0) {
                    break;
                }

                cmx = bx + validWidth - 1; // Ограничиваем ширину для шагов ниже

                int area = validWidth * (y - by + 1);
                if (area > maxArea) {
                    maxArea = area;
                    maxY2 = y;
                    maxX2 = cmx;
                }
            }
        }

        private void mergeTiles() {
            minX = Math.max(0, -MARGIN + (int) Math.floor(viewport.x / blockSize));
            minY = Math.max(0, -MARGIN + (int) Math.floor(viewport.y / blockSize));
            maxX = Math.min(world.sizeX, MARGIN + (int) Math.ceil((viewport.x + viewport.width) / blockSize));
            maxY = Math.min(world.sizeY, MARGIN + (int) Math.ceil((viewport.y + viewport.height) / blockSize));

            int newRows = maxY - minY + 1;
            int newCols = maxX - minX + 1;

            if ((newRows*newCols) != (rows*cols)) {
                processed = createBitSet(newRows * newCols);
                merged = createBitSet(newRows * newCols);
            } else {
                Arrays.fill(merged, 0);
                Arrays.fill(processed, 0);
            }
            rows = newRows;
            cols = newCols;

            rects.clear();

            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    int value = world.getBlockId(x, y);
                    if (value <= 0) {
                        setBit(processed, pos2index(x, y));
                        continue;
                    }
                    if (!world.getRootBlockPosTo(x, y, pos)) {
                        continue;
                    }
                    var bl = content.blocksRegistry.typeById(value);

                    int rx = pos.x;
                    int ry = pos.y;

                    int tx = rx + bl.tileCountX;
                    int ty = ry + bl.tileCountY;

                    for (int i = y; i < ty; i++) {
                        int start = pos2index(x, i);
                        int end = pos2index(tx, i);
                        FixedBitset.setRange(processed, start, end);
                    }
                }
            }

            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    if (isProcessed(x, y)) continue;

                    short value = (short) world.getBlockId(x, y);
                    findMaxRectangleFrom(x, y);
                    if (maxArea <= 1) {
                        continue;
                    }
                    int shadow = colorFor(x, y).rgba8888();
                    var blocks = rects.computeIfAbsent(value, k -> new ArrayList<>());

                    short w = (short)(maxX2 - x + 1);
                    short h = (short)(maxY2 - y + 1);

                    blocks.add(new MergedTile(value, shadow, x, y, w, h));

                    for (int i = y; i <= maxY2; i++) {
                        int start = pos2index(x, i);
                        int end = pos2index(maxX2, i) + 1; // не включается

                        FixedBitset.setRange(processed, start, end);
                        FixedBitset.setRange(merged, start, end);
                    }
                }
            }

            pushState(() -> {
                shader(Shaders.repeat);
                rects.forEach((tileId, tiles) -> {
                    var obj = content.blocksRegistry.typeById(tileId);
                    var tex = obj.texture;

                    for (var tr : tiles) {
                        color(tr.shadowRgba8888);
                        drawRepeated(tex, tr.x, tr.y, tr.w, tr.h);
                    }
                });
                // скидываем всё что ранее подготовили
                pushRenderList();
                flush();
            });

            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    if (!isSet(merged, pos2index(x, y)) && world.getBlockId(x, y) > 0) {
                        var obj = world.getBlock(x, y);
                        int hp = world.getHp(x, y);
                        drawBlock(x, y, obj, hp);
                    }
                }
            }

        }

        boolean USE_DEFAULT = true;

        void draw() {
            camera.getBoundsTo(viewport);
            if (Global.input.justPressed(GLFW.GLFW_KEY_L)) {
                USE_DEFAULT = !USE_DEFAULT;
            }

            if (USE_DEFAULT) {
                collectScene();
                return;
            }

            // StackfulRender.pushRList();
            // Render.queue().flush();

            if (drawStateChanged()) {
                lastBounds.set(viewport);
                lastPreviewBlocks = previewBlocks.size();
                drawStateChanged = false;

                // renderList.setDirty(true);
                // renderList.clear();

                // renderList.begin();

                // StackfulRender.rlist(renderList);
                mergeTiles();
                // renderList.end();
            } else {
                // renderList.setDirty(false);
            }
            // Render.queue().push(renderList);
            // Render.queue().flush();
        }

        private boolean drawStateChanged() {
            return true ||
                   drawStateChanged ||
                   lastPreviewBlocks != previewBlocks.size() ||
                   !viewport.equalsEps(lastBounds, 1e-4f);
        }

        private void collectScene() {
            int minX = (int) Math.floor((viewport.x - blockSize) / blockSize);
            int maxX = (int) Math.floor((viewport.x + viewport.width + blockSize) / blockSize);
            int minY = (int) Math.floor((viewport.y - blockSize) / blockSize);
            int maxY = (int) Math.floor((viewport.y + viewport.height + blockSize) / blockSize);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (!world.inBounds(x, y)) {
                        continue;
                    }
                    int blockId = world.getBlockId(x, y);
                    if (blockId <= 0) {
                        continue;
                    }
                    var obj = world.getBlock(x, y);
                    int hp = world.getHp(x, y);
                    drawBlock(x, y, obj, hp);
                }
            }
        }

        private void drawBlock(int x, int y, StaticObjectsConst obj, int hp) {
            int wx = x * blockSize;
            int wy = y * blockSize;

            if (world.getData(x, y) instanceof TileData.MultiblockPart part) {
                drawDamage(obj, hp, wx, wy);
                // TODO drawPart ?
            } else {
                drawBlock0(x, y, obj, hp);
            }
        }

        private void drawBlock0(int x, int y, StaticObjectsConst obj, int hp) {
            int wx = x * blockSize;
            int wy = y * blockSize;

            Color color = colorFor(x, y);

            StackfulRender.draw(obj.texture, color, wx, wy);
            drawDamage(obj, hp, wx, wy);

            var blockEntity = world.getEntity(x, y);
            if (blockEntity != null && blockEntity.drawStateChanged()) {
                drawStateChanged = true;
                blockEntity.draw(renderList);
            }
        }

        public boolean isSameShadowAndTemp(int x, int y,
                                           int ox, int oy) {

            int c1 = colorFor(x, y).rgba8888();
            int c2 = colorFor(ox, oy).rgba8888();
            return c1 == c2;
        }

        private Color colorFor(int x, int y) {
            Color color = ShadowMap.getColorTo(x, y, tmp);
            final int upperLimit = 100;
            final int lowestLimit = -20;
            final int maxColor = 65;
            float temp = TemperatureMap.getTemp(x, y);

            int a;
            if (temp > upperLimit) {
                a = (int) Math.min(maxColor, Math.abs((temp - upperLimit) / 3));
                color.set(color.r(), color.g() - (a / 2), color.b() - a, color.a());
            } else if (temp < lowestLimit) {
                a = (int) Math.min(maxColor, Math.abs((temp + lowestLimit) / 3));
                color.set(color.r() - a, color.g() - (a / 2), color.b(), color.a());
            }
            return color;
        }
    }

    private static final Chunk chunk = new Chunk();

    public static void drawBlocks() {
        var blockPos = input.mouseBlockPos();
        drawBuildGrid(blockPos.x, blockPos.y);
        chunk.draw();
        for (BlockPreview q : previewBlocks) {
            drawPreviewBlocks(q.x, q.y, q.blockId, q.hp, q.canBreak);
        }
        previewBlocks.clear();
    }

    private static void drawPreviewBlocks(int x, int y, short blockId, byte hp, boolean canBreak) {
        if (blockId <= 0) {
            return;
        }
        var block = Global.content.blocksRegistry.typeById(blockId);

        int wx = x * blockSize;
        int wy = y * blockSize;

        if (viewport.overlaps(wx, wy, block.texture.width(), block.texture.height())) {
            Color color = ShadowMap.getColorTo(x, y, tmp);
            int a = (color.r() + color.g() + color.b()) / 3;
            if (canBreak) {
                color.set(Math.max(0, a - 150), Math.max(0, a - 150), a, 255);
            } else {
                color.set(a, Math.max(0, a - 150), Math.max(0, a - 150), 255);
            }

            draw(block.texture, color, wx, wy);
            drawDamage(block, hp, wx, wy);
        }
    }

    public static void addBlockPreview(int blockX, int blockY, short blockId, byte hp, boolean breakable) {
        previewBlocks.add(new BlockPreview(blockX, blockY, blockId, hp, breakable));
    }

    public static void drawEntities() {
        z(Render.LAYER_ENTITIES);

        for (var ent : entityPool.entities().values()) {
            if (ent instanceof DrawComponent d) {
                ent.getHitboxTo(hitbox);

                if (ent == player) {
                    d.draw(player.x());
                } else {
                    float rightBorder = (world.sizeX - SWAP_AREA) * blockSize;
                    float leftBorder = SWAP_AREA * blockSize;
                    float dx = rightBorder - leftBorder;


                    float drawX = ent.x();
                    // |swap|swap|
                    //      ^ rightBorder
                    //           ^  rightBorder + swap*blockSize
                    // ^ rightBorder - swap*blockSize

                    float rightmostX = ent.x() + hitbox.width;
                    if (rightmostX >= (rightBorder - SWAP_AREA * blockSize) && rightmostX <= (rightBorder + SWAP_AREA * blockSize) &&
                        !viewport.overlaps(ent.x(), ent.y(), hitbox.width, hitbox.height)) {
                        drawX -= dx;
                    } else if (ent.x() >= (leftBorder - SWAP_AREA * blockSize) && ent.x() <= (leftBorder + SWAP_AREA * blockSize) &&
                               !viewport.overlaps(ent.x(), ent.y(), hitbox.width, hitbox.height)) {
                        drawX += dx;
                    }
                    if (viewport.overlaps(drawX, ent.y(), hitbox.width, hitbox.height)) {
                        d.draw(drawX);
                    }
                }
            }
        }
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

    private static void drawDamage(StaticObjectsConst obj, int hp, int x, int y) {
        if (hp > obj.maxHp / 1.5f) {
            // ???
        } else if (hp < obj.maxHp / 3) {
            draw(atlas.get("textures/blocks/damaged1"), x * blockSize, y * blockSize);
        } else {
            draw(atlas.get("textures/blocks/damaged0"), x * blockSize, y * blockSize);
        }
    }
}
