package core.graphic;

import core.GameState;
import core.content.entity.CreatureEntity;
import core.util.BatchScope;

import java.util.HashMap;

import static core.Global.*;
import static core.graphic.WorldDrawing.viewport;

public class ShadowMap {
    private static byte[] shadows;

    private static final Color deletedColor = Color.CLEAR.copy();
    private static final Color addedColor = Color.CLEAR.copy();
    private static final Color
            white = new Color(255, 255, 255, 255),
            brightDirty = new Color(165, 165, 165, 255),
            dirty = new Color(85, 85, 85, 255),
            blackDirty = core.ui.Styles.DIRTY_BRIGHT_BLACK.copy();

    private static final HashMap<CreatureEntity, Color> shadowsDynamic = new HashMap<>();
    private static final Color deletedColorDynamic = Color.CLEAR.copy();
    private static final Color addedColorDynamic = Color.CLEAR.copy();


    // todo переписать генерацию и обновление теней

    public static void init() {
        shadows = new byte[world.sizeX * world.sizeY];
    }

    public static void getShadowTo(int x, int y, Color out) {
        if (world.inBounds(x, y)) {
            switch (shadows[x + world.sizeX * y]) {
                case 0 -> out.set(white);
                case 1 -> out.set(brightDirty);
                case 2 -> out.set(dirty);
                case 3 -> out.set(blackDirty);
            }
        }
    }

    public static int getRawShadow(int x, int y) {
        return shadows[x + world.sizeX * y];
    }

    public static void setShadow(int x, int y, Color color) {
        setShadow(x, y, color.rgba8888());
    }

    public static void setShadow(int x, int y, int rgba8888) {
        if (world.inBounds(x, y)) {
            setShadow0(x, y, rgba8888);
        }
    }

    private static void setShadow0(int x, int y, int rgba8888) {
        shadows[x + world.sizeX * y] = (byte) rgba8888;
    }

    public static int getDegree(int x, int y) {
        if (world.inBounds(x, y)) {
            return shadows[x + world.sizeX * y];
        }
        return 0;
    }

    public static void generate() {
        var scope = new BatchScope(world.genPool);
        scope.submit(1, world.sizeY, (loY, hiY) -> {
            for (int y = loY; y < hiY; y++) {
                for (int x = 0; x < world.sizeX; x++) {
                    if (checkHasGasAround(x, y, 1)) {
                        setShadow0(x, y, 1);
                    }
                }
            }
        }).awaitAll();
        scope.submit(1, world.sizeY, (loY, hiY) -> {
            for (int y = loY; y < hiY; y++) {
                for (int x = 0; x < world.sizeX; x++) {
                    if (checkHasGasAround(x, y, 1) && checkHasDegreeAround(x, y, 1)) {
                        setShadow0(x, y, 2);
                    }
                }
            }
        }).awaitAll();
        scope.submit(2, world.sizeY, (loY, hiY) -> {
            for (int y = loY; y < hiY; y++) {
                for (int x = 0; x < world.sizeX; x++) {
                    if (checkHasDegreeAround(x, y, 2) && checkHasGasAround(x, y, 2)) {
                        setShadow0(x, y, 3);
                    }
                }
            }
        }).awaitAll();
    }

    public static void update() {
        if (gameState == GameState.PLAYING) {
            updateShadows();
        }
    }

    private static void updateShadows() {
        camera.boundsTo(viewport);
        viewport.clampToWorld();
        short minX = viewport.blockMinX();
        short minY = viewport.blockMinY();
        short maxX = viewport.blockMaxX();
        short maxY = viewport.blockMaxY();

        for (short y = minY; y < maxY; y++) {
            for (short x = minX; x < maxX; x++) {
                if (checkHasGasAround(x, y, 1)) {
                    setShadow0(x, y, 1);
                } else {
                    setShadow0(x, y, 0);
                }
            }
        }

        for (short y = minY; y < maxY; y++) {
            for (short x = minX; x < maxX; x++) {
                if (checkHasGasAround(x, y, 1) && checkHasDegreeAround(x, y, 1)) {
                    setShadow0(x, y, 2);
                }
            }
        }

        for (short y = minY; y < maxY; y++) {
            for (short x = minX; x < maxX; x++) {
                if (checkHasDegreeAround(x, y, 2) && checkHasGasAround(x, y, 2)) {
                    setShadow0(x, y, 3);
                }
            }
        }
    }

    public static Color getColorTo(int x, int y, Color out) {
        getShadowTo(x, y, out);
        out.add(addedColor);
        out.sub(deletedColor);
        return out;
    }

    public static Color getColorDynamic(CreatureEntity object) {
        Color color = new Color(shadowsDynamic.computeIfAbsent(object, k -> new Color(Color.WHITE)));
        color.add(addedColorDynamic);
        color.sub(deletedColorDynamic);
        return color;
    }

    public static void addAllColor(Color color) {
        addedColor.set(color);
    }

    public static void addAllColorDynamic(Color color) {
        addedColorDynamic.set(color);
    }

    public static void deleteAllColor(int rgba8888) {
        deletedColor.setRgba8888(rgba8888);
    }

    public static void deleteAllColorDynamic(int rgba8888) {
        deletedColorDynamic.setRgba8888(rgba8888);
    }

    private static boolean isNotGas(int x, int y) {
        return world.getBlockId(x, y) > 0;
        // int id = world.getBlockId(x, y);
        // return id > 0 && content.blocksRegistry.typeById(id).type != Type.GAS;

        // Медленнее:
        // return world.getBlockType(x, y) != Type.GAS;
    }

    private static boolean checkHasGasAround(int x, int y, int radius) {
        return isNotGas(x, y) &&
                isNotGas(x - radius, y) &&
                isNotGas(x + radius, y) &&
                isNotGas(x, y - radius) &&
                //соблюдайте порядок проверки!!! y + radius весомо тяжелее остальных
                //мне так профайлер напел
                isNotGas(x, y + radius);
    }

    private static boolean checkHasDegreeAround(int x, int y, int radius) {
        return getDegree(x - radius, y) > 0 &&
                getDegree(x + radius, y) > 0 &&
                getDegree(x, y - radius) > 0 &&
                getDegree(x, y + radius) > 0;
    }
}
