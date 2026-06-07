package core.graphic;

import core.GameState;
import core.UI.Styles;
import core.content.blocks.Block.Type;
import core.content.entity.CreatureEntity;
import core.content.entity.Hitbox;

import java.util.HashMap;
import java.util.stream.IntStream;

import static core.Global.*;
import static core.WorldCoordinates.toBlock;
import static core.graphic.WorldDrawing.viewport;

public class ShadowMap {
    private static byte[] shadows;
    private static HashMap<CreatureEntity, Color> shadowsDynamic = new HashMap<>();
    private static Color deletedColor = Color.CLEAR, deletedColorDynamic = Color.CLEAR, addedColor = Color.CLEAR, addedColorDynamic = Color.CLEAR;
    private final static Color white = new Color(255, 255, 255, 255),
            brightDirty = new Color(165, 165, 165, 255),
            dirty = new Color(85, 85, 85, 255),
            blackDirty = Styles.DIRTY_BRIGHT_BLACK.copy();


    // todo переписать генерацию и обновление теней

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
        shadows = new byte[world.sizeX * world.sizeY];
        int totalColumns = world.sizeX;
        int cores = Runtime.getRuntime().availableProcessors();
        int chunkSize = (totalColumns / cores) + 1;

        IntStream.range(0, cores).parallel().forEach(p -> {
            int startChunkX = Math.max(1, p * chunkSize);
            int endChunkX = Math.min(world.sizeX - 1, startChunkX + chunkSize);

            for (int y = 1; y < world.sizeY; y++) {
                for (int x = startChunkX; x < endChunkX; x++) {
                    if (checkHasGasAround(x, y, 1)) {
                        setShadow0(x, y, 1);
                    }
                }
            }
        });

        IntStream.range(0, cores).parallel().forEach(p -> {
            int startChunkX = Math.max(1, p * chunkSize);
            int endChunkX = Math.min(world.sizeX - 1, startChunkX + chunkSize);
            for (int y = 1; y < world.sizeY; y++) {
                for (int x = startChunkX; x < endChunkX; x++) {
                    if (checkHasGasAround(x, y, 1) && checkHasDegreeAround(x, y, 1)) {
                        setShadow0(x, y, 2);
                    }
                }
            }
        });

        IntStream.range(0, cores).parallel().forEach(p -> {
            int startChunkX = Math.max(2, p * chunkSize);
            int endChunkX = Math.min(world.sizeX - 2, startChunkX + chunkSize);
            for (int y = 2; y < world.sizeY; y++) {
                for (int x = startChunkX; x < endChunkX; x++) {
                    if (checkHasDegreeAround(x, y, 2) && checkHasGasAround(x, y, 2)) {
                        setShadow0(x, y, 3);
                    }
                }
            }
        });
    }

    public static void update() {
        if (gameState == GameState.PLAYING) {
            updateShadows();
        }
    }

    private static void updateShadows() {
        camera.getBoundsTo(viewport);
        int minX = Math.max(0, toBlock(viewport.x));
        int minY = Math.max(0, toBlock(viewport.y));
        int maxX = Math.min(world.sizeX - 1, toBlock(viewport.x + viewport.width));
        int maxY = Math.min(world.sizeY - 1, toBlock(viewport.y + viewport.height));

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (checkHasGasAround(x, y, 1)) {
                    setShadow0(x, y, 1);
                } else {
                    setShadow0(x, y, 0);
                }
            }
        }

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (checkHasGasAround(x, y, 1) && checkHasDegreeAround(x, y, 1)) {
                    setShadow0(x, y, 2);
                }
            }
        }

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
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
        addedColor = color;
    }

    public static void addAllColorDynamic(Color color) {
        addedColorDynamic = color;
    }

    public static void deleteAllColor(Color color) {
        deletedColor = color;
    }

    public static void deleteAllColorDynamic(Color color) {
        deletedColorDynamic = color;
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
