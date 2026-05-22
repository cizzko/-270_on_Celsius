package core.World.Textures;

import core.GameState;
import core.UI.Styles;
import core.World.StaticWorldObjects.StaticObjectsConst.Type;
import core.content.entity.CreatureEntity;
import core.util.Color;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static core.Global.*;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.World.Textures.TextureDrawing.viewport;

public class ShadowMap {
    private static int[] shadows;
    private static HashMap<CreatureEntity, Color> shadowsDynamic = new HashMap<>();
    private static Color deletedColor = Color.CLEAR, deletedColorDynamic = Color.CLEAR, addedColor = Color.CLEAR, addedColorDynamic = Color.CLEAR;

    private static final Color tmp = new Color();

    // todo переписать генерацию и обновление теней

    public static void getShadowTo(int x, int y, Color out) {
        if (x >= 0 && y >= 0 && x < world.sizeX && y < world.sizeY) {
            out.setRgba8888(shadows[x + world.sizeX * y]);
        } else {
            out.set(Color.CLEAR);
        }
    }

    public static int getRawShadow(int x, int y) {
        return shadows[x + world.sizeX * y];
    }

    public static void setShadow(int x, int y, Color color) {
        setShadow(x, y, color.rgba8888());
    }

    public static void setShadow(int x, int y, int rgba8888) {
        if (x < 0 || y < 0 || x >= world.sizeX || y >= world.sizeY) {
            return;
        }
        setShadow0(x, y, rgba8888);
    }

    private static void setShadow0(int x, int y, int rgba8888) {
        shadows[x + world.sizeX * y] = rgba8888;
    }

    public static int getDegree(int x, int y) {
        getShadowTo(x, y, tmp);
        int rgb = tmp.r() + tmp.g() + tmp.b();
        return (int) Math.abs(Math.ceil(rgb / 198f - 4));
    }

    public static CompletableFuture<Void> generate() {
        shadows = new int[world.sizeX * world.sizeY];
        Arrays.fill(shadows, Color.white);

        return generateShadows();
    }

    private static CompletableFuture<Void> generateShadows() {
        return CompletableFuture.runAsync(() -> {
            int totalColumns = world.sizeX;
            int cores = Runtime.getRuntime().availableProcessors();
            int chunkSize = (totalColumns / cores) + 1;

            world.genPool.submit(() -> IntStream.range(0, cores).parallel().forEach(p -> {
                int startChunkX = Math.max(2, p * chunkSize);
                int endChunkX = Math.min(world.sizeX - 2, startChunkX + chunkSize);
                int dirtyBrightBlack = Styles.DIRTY_BRIGHT_BLACK.rgba8888();
                for (int y = 2; y < world.sizeY; y++) {
                    for (int x = startChunkX; x < endChunkX; x++) {
                        if (checkHasDegreeAround(x, y, 2) && checkHasGasAround(x, y, 2)) {
                            setShadow0(x, y, dirtyBrightBlack);
                        }
                    }
                }
            })).join();

            world.genPool.submit(() -> IntStream.range(0, cores).parallel().forEach(p -> {
                int shadowWhite = Color.rgba8888(165, 165, 165, 255);
                int startChunkX = Math.max(1, p * chunkSize);
                int endChunkX = Math.min(world.sizeX - 1, startChunkX + chunkSize);
                for (int y = 1; y < world.sizeY; y++) {
                    for (int x = startChunkX; x < endChunkX; x++) {
                        if (checkHasGasAround(x, y, 1)) {
                            setShadow0(x, y, shadowWhite);
                        } else {
                            setShadow0(x, y, Color.white);
                        }
                    }
                }
            })).join();

            world.genPool.submit(() -> IntStream.range(0, cores).parallel().forEach(p -> {
                int shadowDirtWhite = Color.rgba8888(85, 85, 85, 255);
                int startChunkX = Math.max(1, p * chunkSize);
                int endChunkX = Math.min(world.sizeX - 1, startChunkX + chunkSize);
                for (int x = startChunkX; x < endChunkX; x++) {
                    for (int y = 1; y < world.sizeY; y++) {
                        if (checkHasGasAround(x, y, 1) && checkHasDegreeAround(x, y, 1)) {
                            setShadow0(x, y, shadowDirtWhite);
                        }
                    }
                }
            })).join();

        }, world.genPool);
    }

    public static void update() {
        if (gameState == GameState.PLAYING) {
            updateShadows();
        }
    }

    private static void updateShadows() {
        camera.getBoundsTo(viewport);
        int minX = Math.max(0, (int) Math.floor((viewport.x - blockSize) / blockSize));
        int minY = Math.max(0, (int) Math.floor((viewport.y - blockSize) / blockSize));
        int maxX = Math.min(world.sizeX,(int) Math.floor((viewport.x + viewport.width + blockSize) / blockSize));
        int maxY = Math.min(world.sizeY, (int) Math.floor((viewport.y + viewport.height + blockSize) / blockSize));

        int c1 = Color.rgba8888(165, 165, 165, 255);
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (checkHasGasAround(x, y, 1)) {
                    setShadow(x, y, c1);
                } else {
                    setShadow(x, y, Color.white);
                }
            }
        }

        int c2 = Color.rgba8888(85, 85, 85, 255);
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (checkHasGasAround(x, y, 1) && checkHasDegreeAround(x, y, 1)) {
                    setShadow0(x, y, c2);
                }
            }
        }

        int dirtyBrightBlack = Styles.DIRTY_BRIGHT_BLACK.rgba8888();
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (checkHasDegreeAround(x, y, 2) && checkHasGasAround(x, y, 2)) {
                    setShadow0(x, y, dirtyBrightBlack);
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
        int id = world.getBlockId(x, y);
        return id > 0 && content.blocksRegistry.typeById(id).type != Type.GAS;
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