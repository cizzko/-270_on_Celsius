package core.graphic;

import core.Global;
import core.Window;
import core.content.blocks.Block;
import core.World.StaticWorldObjects.TemperatureMap;
import core.content.blocks.data.TileData;
import core.content.entity.DrawComponent;
import core.g2d.*;
import core.math.Point2i;
import core.math.Rectangle;
import core.math.TmpShapes;
import core.util.FixedBitset;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;

import static core.Constants.World.SWAP_AREA;
import static core.Global.*;
import static core.Global.player;
import static core.World.Creatures.Player.Inventory.Inventory.drawBuildGrid;
import static core.WorldCoordinates.toBlock;
import static core.WorldCoordinates.toWorld;
import static core.g2d.StackfulRender.*;
import static core.g2d.StackfulRender.flush;
import static core.util.FixedBitset.*;
import static core.util.FixedBitset.isSet;

/// Рендер всего в мире. Используйте на свой страх и риск в интерфейсе
public final class WorldDrawing {
    private WorldDrawing() {}

    static final Rectangle viewport = new Rectangle(); // TODO убрать в Camera2

    public static void drawPreviewBlocks() {
        for (var q : PREVIEW_BLOCKS) {
            drawPreviewBlocks(q.x, q.y, q.blockId, q.hp, q.canBreak);
        }
        PREVIEW_BLOCKS.clear();
    }

    private static void drawPreviewBlocks(int x, int y, short blockId, byte hp, boolean canBreak) {
        if (blockId <= 0) {
            return;
        }
        var block = Global.content.blocksRegistry.typeById(blockId);

        if (viewport.overlaps(x, y, block.tileCountX, block.tileCountY)) {
            Color color = ShadowMap.getColorTo(x, y, TmpShapes.c1);
            int a = (color.r() + color.g() + color.b()) / 3;
            if (canBreak) {
                color.set(Math.max(0, a - 150), Math.max(0, a - 150), a, 255);
            } else {
                color.set(a, Math.max(0, a - 150), Math.max(0, a - 150), 255);
            }

            draw(block.texture, color, x, y, block.tileCountX, block.tileCountY);
            drawDamage(block, hp, x, y);
        }
    }

    public static void addBlockPreview(int blockX, int blockY, short blockId, byte hp, boolean breakable) {
        PREVIEW_BLOCKS.add(new BlockPreview(blockX, blockY, blockId, hp, breakable));
    }

    public static void drawBlocks() {
        var blockPos = input.mouseBlockPos();
        drawBuildGrid(blockPos.x, blockPos.y);
        chunk.draw();
        WorldDrawing.drawPreviewBlocks();
    }

    public static void resetState() {
        PREVIEW_BLOCKS.clear();
    }

    public record BlockPreview(int x, int y, short blockId, byte hp, boolean canBreak) {}

    public static final ArrayList<BlockPreview> PREVIEW_BLOCKS = new ArrayList<>();

    public static void drawGameText(float x, float y, CharSequence text, int rgba8888) {
        float startX = x;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == '\\' && i + 1 < text.length() && text.charAt(i + 1) == 'n') {
                y -= toWorld(30);
                i++;
                x = startX;
                continue;
            }
            Font.Glyph glyph = Window.defaultFont.getGlyph(ch);
            draw(glyph, rgba8888, x, y, toWorld(glyph.width()), toWorld(glyph.height()));
            x += toWorld(5);
        }
    }

    private static void drawDamage(Block obj, int hp, int bx, int by) {
        if (hp > obj.maxHp / 1.5f) {
            // ???
        } else if (hp < obj.maxHp / 3) {
            draw(atlas.get("textures/blocks/damaged1"), bx, by, obj.tileCountX, obj.tileCountY);
        } else {
            draw(atlas.get("textures/blocks/damaged0"), bx, by, obj.tileCountX, obj.tileCountY);
        }
    }

    private static class Chunk {
        private final Point2i pos = new Point2i();

        static final int MARGIN = 2;

        record MergedTile(short blockId, int shadowRgba8888, int x, int y, short w, short h) {}

        int rows, cols;
        int minX, minY;
        int maxX, maxY;
        long[] processed;
        long[] merged;

        final Short2ObjectOpenHashMap<ArrayList<Chunk.MergedTile>> rects = new Short2ObjectOpenHashMap<>();

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

        private void mergingDraw() {
            int newRows = maxY - minY + 1;
            int newCols = maxX - minX + 1;

            if ((newRows*newCols) != (rows*cols)) {
                assert Math.toIntExact(Math.multiplyFull(newCols, newRows)) > 0;
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

                    blocks.add(new Chunk.MergedTile(value, shadow, x, y, w, h));

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

        private void drawBlock(int x, int y, Block obj, int hp) {
            if (world.getData(x, y) instanceof TileData.MultiblockPart part) {
                drawDamage(obj, hp, x, y);
                // TODO drawPart ?
            } else {
                drawBlock0(x, y, obj, hp);
            }
        }

        private void drawBlock0(int x, int y, Block obj, int hp) {
            Color color = colorFor(x, y);

            StackfulRender.draw(obj.texture, color.rgba8888(), x, y, obj.tileCountX, obj.tileCountY);
            drawDamage(obj, hp, x, y);

            var blockEntity = world.getEntity(x, y);
            if (blockEntity != null) {
                blockEntity.draw();
            }
        }

        boolean USE_DEFAULT = false;

        void draw() {
            if (Global.input.justPressed(GLFW.GLFW_KEY_L)) {
                USE_DEFAULT = !USE_DEFAULT;
            }

            camera.getBoundsTo(viewport);
            minX = Math.max(0, -MARGIN + toBlock(viewport.x));
            minY = Math.max(0, -MARGIN + toBlock(viewport.y));
            maxX = Math.min(world.sizeX - 1, MARGIN + toBlock(viewport.x + viewport.width));
            maxY = Math.min(world.sizeY - 1, MARGIN + toBlock(viewport.y + viewport.height));

            if (USE_DEFAULT) {
                notMergingDraw();
            } else {
                mergingDraw();
            }
        }

        private void notMergingDraw() {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    int blockId = world.getBlockId(x, y);
                    if (blockId <= 0) {
                        continue;
                    }
                    var obj = content.blocksRegistry.typeById(blockId);
                    int hp = world.getHp(x, y);
                    drawBlock(x, y, obj, hp);
                }
            }
        }

        public boolean isSameShadowAndTemp(int x, int y,
                                           int ox, int oy) {

            int c1 = colorFor(x, y).rgba8888();
            int c2 = colorFor(ox, oy).rgba8888();
            return c1 == c2;
        }
    }

    private static Color colorFor(int x, int y) {
        Color color = ShadowMap.getColorTo(x, y, TmpShapes.c1);
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

    private static final Chunk chunk = new Chunk();

    // region Entities
    public static void drawEntities() {
        camera.getBoundsTo(viewport);
        var hitbox = TmpShapes.r1;
        entityPool.forEach(ent -> {
            if (ent instanceof DrawComponent d) {
                ent.getHitboxTo(hitbox);

                if (ent == player) {
                    d.draw(player.x());
                } else {
                    float rightBorder = world.sizeX - SWAP_AREA;
                    float leftBorder = SWAP_AREA;
                    float dx = rightBorder - leftBorder;


                    float drawX = ent.x();
                    // |swap|swap|
                    //      ^ rightBorder
                    //           ^  rightBorder + swap
                    // ^ rightBorder - swap

                    float rightmostX = ent.x() + hitbox.width;
                    if (rightmostX >= (rightBorder - SWAP_AREA) && rightmostX <= (rightBorder + SWAP_AREA) &&
                        !viewport.overlaps(ent.x(), ent.y(), hitbox.width, hitbox.height)) {
                        drawX -= dx;
                    } else if (ent.x() >= (leftBorder - SWAP_AREA) && ent.x() <= (leftBorder + SWAP_AREA) &&
                               !viewport.overlaps(ent.x(), ent.y(), hitbox.width, hitbox.height)) {
                        drawX += dx;
                    }
                    if (viewport.overlaps(drawX, ent.y(), hitbox.width, hitbox.height)) {
                        d.draw(drawX);
                    }
                }
            }
        });
    }
    // endregion
}
