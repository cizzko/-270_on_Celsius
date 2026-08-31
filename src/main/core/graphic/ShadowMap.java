package core.graphic;

import core.GameState;
import core.Global;
import core.World.Weather.Sun;
import core.math.Point2i;
import core.math.TmpShapes;
import core.util.Debug;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static core.Global.*;
import static core.WorldCoordinates.*;
import static core.graphic.WorldDrawing.viewport;
import static core.math.MathUtil.lerp;
import static java.lang.Math.*;

public final class ShadowMap {

    private static final Color sunFade = new Color(0xFF0000);

    private static final Logger log = LogManager.getLogger(ShadowMap.class);

    static byte[] light, tmp;
    static byte[] tileTrans, tileEmission; // исходные от блоков; числовые проценты [0,100]
    static byte[] trans, diffusion;        // предвычисленные значения
    static short[] lightHeightMap;

    static int rows = 0, cols = 0;
    static short minX, minY, maxX, maxY;

    static boolean dirty;

    static final float SQRT2 = (float) sqrt(2);

    // Коэффициенты затухания за одну клетку
    // Чем меньше - тем быстрее темнеет.
    static final float FALLOFF_AIR    = 0.91f; // свет почти не теряется в воздухе
    static final float FALLOFF_SOLID;          // в твёрдом блоке гаснет быстро

    static final float MIN_LIGHT = 10 / 255f;

    // Радиус влияния одного источника (в блоках)
    static int LIGHT_RADIUS;

    static final float SOLID_VISIBLE_DEPTH = 0.95f;
    static float VISIBLE_THRESHOLD = 0.42f;

    static final int MARGIN = 42;
    static final Point2i lastCameraPos = new Point2i(-1, -1);

    static {
        FALLOFF_SOLID = (float) pow(VISIBLE_THRESHOLD, 1.0 / SOLID_VISIBLE_DEPTH);
        LIGHT_RADIUS = (int) ceil(log(MIN_LIGHT) / log(FALLOFF_AIR));
    }

    static int pos2index(int x, int y) {
        int dx = x - minX;
        int dy = y - minY;
        if (Debug.debugLevel > 0)
            if (dx < 0 || dx >= cols || dy < 0 || dy >= rows)
                throw new IllegalArgumentException("Coordinates out of current ShadowMap bounds: " + cols + " x " + rows + " : " + x + ", " + y);
        return dx + cols * dy;
    }

    static boolean inBounds(int x, int y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    private static float checkAdjacentLight(int x, int y, float skyLight) {
        return inBounds(x, y) && seesSkyNotBlock(x, y)
                ? skyLight
                : 0;
    }

    private static float skyLight(int x) {
        return Sun.sunLightAt(x);
                // 1f - sunFade.rf();
    }

    private static float adjacentLight(int x, int y, float skyLight) {
        return max(checkAdjacentLight(x - 1, y, skyLight), checkAdjacentLight(x + 1, y, skyLight));
    }

    private static float applySkyLight(int x, int y, float skyLight) {
        return seesSky(x, y) ? skyLight : 0f;
    }

    private static boolean seesSkyNotBlock(int x, int y) {
        short t = Global.world.tiles[Global.world.pos2index(x, y)];
        if (tileTrans[t] < 100)
            return false;
        return y >= lightHeightMap[x];
    }

    private static boolean seesSky(int x, int y) {
        return y >= lightHeightMap[x];
    }

    public static void updateByCameraMove() {
        var camPos = camera.position;
        var newCameraPos = TmpShapes.p1.set(toBlock(camPos.x), toBlock(camPos.y));

        if (dirty || !newCameraPos.equals(lastCameraPos)) {
            dirty = false;

            lastCameraPos.set(newCameraPos);

            updateViewport();

            long t = System.currentTimeMillis();
            recalcRegion(minX, minY, maxX, maxY);
            log.trace("ShadowMap(ByCameraMove): {}ms",
                    System.currentTimeMillis() - t);
        }
    }

    private static void updateViewport() {
        camera.boundsTo(viewport);
        viewport.floorToBlock();
        viewport.clampToWorldMargin(MARGIN);

        minX = viewport.blockMinX();
        minY = viewport.blockMinY();
        maxX = viewport.blockMaxX();
        maxY = viewport.blockMaxY();

        int newRows = maxY - minY + 1;
        int newCols = maxX - minX + 1;

        if (newRows * newCols != rows * cols) {
            rows = newRows;
            cols = newCols;
            light = new byte[rows * cols];
            tmp   = new byte[rows * cols];
        }
    }

    public static void setDirty(int x, int y, int tileCountX, int tileCountY) {
        if (gameState != GameState.PLAYING)
            return;

        int blockX1 = x + tileCountX - 1;
        int blockY1 = y + tileCountY - 1;

        {
            int x0 = max(0, x);
            int y0 = max(0, y);
            int x1 = min(world.sizeX - 1, blockX1);
            int y1 = min(world.sizeY - 1, blockY1);

            updateHeightsFromRange(x0, x1, y0, y1);
        }

        if (blockX1 < minX || x > maxX || blockY1 < minY || y > maxY) {
            return;
        }

        // TODO сделать что-то умное учитывающее область которую ндо обновить
        int x0 = minX;
        int y0 = minY;
        int x1 = maxX;
        int y1 = maxY;
        recalcRegion(x0, y0, x1, y1);
    }

    private static void updateHeightsFromRange(int x0, int x1, int y0, int y1) {
        // TODO необязательно все полоски проверять. Если y0 < старая высота, то можно пропустить
        // для корректности, всё равно это почти бесплатно
        Arrays.fill(lightHeightMap, x0, x1 + 1, (short)0);

        var worl = world;
        for (int x = x0; x <= x1; x++) {
            for (int y = worl.sizeY - 1; y >= 0; y--) {
                int idx = worl.pos2index(x, y);
                if (tileTrans[worl.tiles[idx]] < 100) {
                    lightHeightMap[x] = (short) y;
                    break; // следующий x
                }
            }
        }
    }

    public static Color getEntityColorTo(double x, double y, float width, float height, Color out) {
        double cx = x + width / 2.0;

        int x0 = toBlock(cx);
        int y0 = toBlock(y);

        int x1 = x0 + 1;
        int y1 = y0 + 1;

        float tX = toOffset(cx);
        float tY = toOffset(y);

        float s00 = from_byte(light[pos2index(x0, y0)]); // Bottom-Left
        float s10 = from_byte(light[pos2index(x1, y0)]); // Bottom-Right
        float s01 = from_byte(light[pos2index(x0, y1)]); // Top-Left
        float s11 = from_byte(light[pos2index(x1, y1)]); // Top-Right

        float lightBottom = lerp(s00, s10, tX);
        float lightTop    = lerp(s01, s11, tX);
        float gray        = lerp(lightBottom, lightTop, tY);

        int r = Color.toInt(gray);
        out.set(r, r, r, 255);
        return out;
    }

    private static void recalcRegion(int rx0, int ry0, int rx1, int ry1) {
        var light = ShadowMap.light;

        for (int y = ry0; y <= ry1; y++) {
            int row = cols * (y - minY);
            for (int x = rx0; x <= rx1; x++) {
                int i = row + (x - minX);
                float result;
                short tile = world.tiles[world.pos2index(x, y)];

                float skyLight = skyLight(x);
                float emission = normalize(tileEmission[tile]);
                float sky = applySkyLight(x, y, skyLight);
                if (sky > 0) result = max(sky, emission);
                // else         result = max(adjacentLight(x, y, sky), emission);
                else result = emission;

                light[i] = to_byte(result);
            }
        }

        // слева-сверху -> вправо-вниз
        for (int y = ry0; y <= ry1; y++) {
            int row = cols * (y - minY);
            for (int x = rx0; x <= rx1; x++) {
                int i = row + (x - minX);
                float v = from_byte(light[i]);
                var tile = world.tiles[world.pos2index(x, y)];
                float f = from_byte(trans[tile]);
                boolean mx = x > minX;
                boolean my = y > minY;
                if (mx) {
                    float n = from_byte(light[i - 1]);
                    if (n > MIN_LIGHT) v = max(v, n * f);
                }
                if (my) {
                    float n = from_byte(light[i - cols]);
                    if (n > MIN_LIGHT) v = max(v, n * f);
                }
                if (mx & my) {
                    float n = from_byte(light[i - cols - 1]);
                    if (n > MIN_LIGHT) v = max(v, n * from_byte(diffusion[tile]));
                }
                light[i] = to_byte(v);
            }
        }

        // справа-снизу -> влево-вверх
        for (int y = ry1; y >= ry0; y--) {
            int row = cols * (y - minY);
            for (int x = rx1; x >= rx0; x--) {
                int i = row + (x - minX);
                float v = from_byte(light[i]);
                var tile = world.tiles[world.pos2index(x, y)];
                float f = from_byte(trans[tile]);
                boolean mx = x < maxX;
                boolean my = y < maxY;
                if (mx) {
                    float n = from_byte(light[i + 1]);
                    if (n > MIN_LIGHT) v = max(v, n * f);
                }
                if (my) {
                    float n = from_byte(light[i + cols]);
                    if (n > MIN_LIGHT) v = max(v, n * f);
                }
                if (mx & my) {
                    float n = from_byte(light[i + cols + 1]);
                    if (n > MIN_LIGHT) v = max(v, n * from_byte(diffusion[tile]));
                }
                light[i] = to_byte(v);
            }
        }

        // справа-сверху -> влево-вниз
        for (int y = ry0; y <= ry1; y++) {
            int row = cols * (y - minY);
            for (int x = rx1; x >= rx0; x--) {
                int i = row + (x - minX);
                float v = from_byte(light[i]);
                var tile = world.tiles[world.pos2index(x, y)];
                float f = from_byte(trans[tile]);
                boolean mx = x < maxX;
                boolean my = y > minY;
                if (mx) {
                    float n = from_byte(light[i + 1]);
                    if (n > MIN_LIGHT) v = max(v, n * f);
                }
                if (my) {
                    float n = from_byte(light[i - cols]);
                    if (n > MIN_LIGHT) v = max(v, n * f);
                }
                if (mx & my) {
                    float n = from_byte(light[i - cols + 1]);
                    if (n > MIN_LIGHT) v = max(v, n * from_byte(diffusion[tile]));
                }
                light[i] = to_byte(v);
            }
        }

        // слева-снизу -> вправо-вверх
        for (int y = ry1; y >= ry0; y--) {
            int row = cols * (y - minY);
            for (int x = rx0; x <= rx1; x++) {
                int i = row + (x - minX);
                float v = from_byte(light[i]);
                var tile = world.tiles[world.pos2index(x, y)];
                float f = from_byte(trans[tile]);
                boolean mx = x > minX;
                boolean my = y < maxY;
                if (mx) {
                    float n = from_byte(light[i - 1]);
                    if (n > MIN_LIGHT) v = max(v, n * f);
                }
                if (my) {
                    float n = from_byte(light[i + cols]);
                    if (n > MIN_LIGHT) v = max(v, n * f);
                }
                if (mx & my) {
                    float n = from_byte(light[i + cols - 1]);
                    if (n > MIN_LIGHT) v = max(v, n * from_byte(diffusion[tile]));
                }
                light[i] = to_byte(v);
            }
        }

        blurLightRegion(rx0, ry0, rx1, ry1);
    }

    private static void blurLightRegion(int rx0, int ry0, int rx1, int ry1) {
        var src = ShadowMap.light;
        var tmp = ShadowMap.tmp;
        Arrays.fill(tmp, (byte)0);

        for (int y = ry0; y <= ry1; y++) {
            int row = cols * (y - minY);
            for (int x = rx0; x <= rx1; x++) {
                int i = row + (x - minX);

                float sum = from_byte(src[i]) * 4f;
                float w = 4f;

                if (x > minX) {
                    sum += from_byte(src[i - 1]) * 2f;
                    w += 2f;
                }
                if (x < maxX) {
                    sum += from_byte(src[i + 1]) * 2f;
                    w += 2f;
                }

                tmp[i] = to_byte(sum / w);
            }
        }

        for (int y = ry0; y <= ry1; y++) {
            int row = cols * (y - minY);
            for (int x = rx0; x <= rx1; x++) {
                int i = row + (x - minX);

                float sum = from_byte(tmp[i]) * 4f;
                float w = 4f;

                if (y > minY) {
                    sum += from_byte(tmp[i - cols]) * 2f;
                    w += 2f;
                }
                if (y < maxY) {
                    sum += from_byte(tmp[i + cols]) * 2f;
                    w += 2f;
                }

                light[i] = to_byte(sum / w);
            }
        }
    }


    public static void init() {
        lightHeightMap = new short[world.sizeX];

        var blkRegistry = Global.content.blocksRegistry;
        var blocks = blkRegistry.values();
        tileTrans     = new byte[blocks.length];
        tileEmission  = new byte[blocks.length];

        trans         = new byte[blocks.length];
        diffusion     = new byte[blocks.length];

        for (int tileId = 0; tileId < blocks.length; tileId++) {
            var block = blocks[tileId];
            float tran = normalize(block.lightTransmission);
            tileTrans[tileId]     = block.lightTransmission;
            tileEmission[tileId]  = block.lightEmission;

            if (tileId == 0) {
                float range       = (float)pow(FALLOFF_AIR, SQRT2); // [0,1]
                trans[tileId]     = to_byte(FALLOFF_AIR);
                diffusion[tileId] = to_byte(normalize(block.lightDiffusion) * range);
            } else {
                float range       = (float)pow(tran, SQRT2); // [0,1]
                float shadowTrans = lerp(FALLOFF_SOLID, FALLOFF_AIR, tran);
                trans[tileId]     = to_byte(shadowTrans);
                diffusion[tileId] = to_byte(range * shadowTrans); // [0,1] * [0,1] = [0,1]
            }
        }
    }

    static byte  to_byte(float val)  { return (byte) (unjava_clamp(val) * 0xff); }
    static float from_byte(byte val) { return (float)Byte.toUnsignedInt(val) / 0xff; }

    @SuppressWarnings("MathClampMigration")
    static float unjava_clamp(float v) {
        return Math.min(1, Math.max(v, 0));
    }

    static float normalize(int val) {
        return unjava_clamp(((float)val)/100f);
    }

    public static void setSunFade(int rgba8888) {
        if (rgba8888 != sunFade.rgba8888()) {
            dirty = true;
            sunFade.setRgba8888(rgba8888);
        }

    }

    public static void setDirty(boolean value) {
        dirty = value;
    }

    public static Color getColorTo(int x, int y, Color out) {
        if (x < minX || x > maxX || y < minY || y > maxY) {
            out.set(Color.WHITE);
            return out;
        }
        float l = from_byte(light[pos2index(x, y)]);
        int gray = Color.toInt(max(MIN_LIGHT, l));
        out.set(gray, gray, gray, 255);
        return out;
    }

    public static Color rawColorTo(int x, int y, Color out) {
        if (x < minX || x > maxX || y < minY || y > maxY) {
            out.set(Color.WHITE);
            return out;
        }
        int maxLight = Byte.toUnsignedInt(light[pos2index(x, y)]);
        out.set(maxLight, maxLight, maxLight, 255);
        return out;
    }

    public static void updateHeights() {
        Arrays.fill(lightHeightMap, (short)0); // для корректности, всё равно это почти бесплатно

        var worl = world;
        for (int x = 0; x < worl.sizeX; x++) {
            for (int y = worl.sizeY - 1; y >= 0; y--) {
                int idx = worl.pos2index(x, y);
                if (tileTrans[worl.tiles[idx]] < 100) {
                    lightHeightMap[x] = (short) y;
                    break; // следующий x
                }
            }
        }
    }

    public static void updateIfDirty() {
        if (dirty) {
            dirty = false;

            updateViewport();

            long t = System.currentTimeMillis();
            recalcRegion(minX, minY, maxX, maxY);
            log.trace("ShadowMap(ByDirty): {}ms", System.currentTimeMillis() - t);
        }
    }
}
