package core.World.Textures;

import core.GameState;
import core.UI.Styles;
import core.World.Creatures.DynamicWorldObjects;
import core.World.StaticWorldObjects.StaticObjectsConst.Type;
import core.util.Color;

import java.util.Arrays;
import java.util.HashMap;

import static core.Global.*;

public class ShadowMap {
    private static int[] shadows;
    private static HashMap<DynamicWorldObjects, Color> shadowsDynamic = new HashMap<>();
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

    public static void setShadow(int x, int y, Color color) {
        setShadow(x, y, color.rgba8888());
    }

    public static void setShadow(int x, int y, int rgba8888) {
        if (x < 0 || y < 0 || x >= world.sizeX || y >= world.sizeY) {
            return;
        }
        shadows[x + world.sizeX * y] = rgba8888;
    }

    public static int getDegree(int x, int y) {
        getShadowTo(x, y, tmp);
        int rgb = tmp.r() + tmp.g() + tmp.b();
        return (int) Math.abs(Math.ceil(rgb / 198f - 4));
    }

    public static void generate() {
        shadows = new int[world.sizeX * world.sizeY];
        Arrays.fill(shadows, Color.WHITE.rgba8888());

        generateShadows();
    }

    private static void generateShadows() {
        int shadowWhite = Color.rgba8888(165, 165, 165, 255);
        int shadowDirtWhite = Color.rgba8888(85, 85, 85, 255);

        for (int x = 1; x < world.sizeX - 1; x++) {
            for (int y = 1; y < world.sizeY - 1; y++) {
                if (checkHasGasAround(x, y, 1)) {
                    setShadow(x, y, shadowWhite);
                } else {
                    setShadow(x, y, Color.WHITE);
                }
            }
        }

        for (int x = 1; x < world.sizeX - 1; x++) {
            for (int y = 1; y < world.sizeY - 1; y++) {
                if (checkHasGasAround(x, y, 1) && checkHasDegreeAround(x, y, 1)) {
                    setShadow(x, y, shadowDirtWhite);
                }
            }
        }

        for (int x = 2; x < world.sizeX - 2; x++) {
            for (int y = 2; y < world.sizeY - 2; y++) {
                if (checkHasDegreeAround(x, y, 2) && checkHasGasAround(x, y, 2)) {
                    setShadow(x, y, Styles.DIRTY_BRIGHT_BLACK);
                }
            }
        }
    }

    public static void update() {
        if (gameState == GameState.PLAYING) {
            int xPos = (int) player.getX();
            int yPos = (int) player.getY();

            for (int x = xPos / TextureDrawing.blockSize - 20; x < xPos / TextureDrawing.blockSize + 21; x++) {
                for (int y = yPos / TextureDrawing.blockSize - 8; y < yPos / TextureDrawing.blockSize + 16; y++) {
                    if (checkHasGasAround(x, y, 1)) {
                        setShadow(x, y, Color.rgba8888(165, 165, 165, 255));
                    } else {
                        setShadow(x, y, Color.WHITE);
                    }
                }
            }
            for (int x = xPos / TextureDrawing.blockSize - 20; x < xPos / TextureDrawing.blockSize + 21; x++) {
                for (int y = yPos / TextureDrawing.blockSize - 8; y < yPos / TextureDrawing.blockSize + 16; y++) {
                    if (checkHasGasAround(x, y, 1) && checkHasDegreeAround(x, y, 1)) {
                        setShadow(x, y, Color.rgba8888(85, 85, 85, 255));
                    }
                }
            }
            for (int x = xPos / TextureDrawing.blockSize - 20; x < xPos / TextureDrawing.blockSize + 21; x++) {
                for (int y = yPos / TextureDrawing.blockSize - 8; y < yPos / TextureDrawing.blockSize + 16; y++) {
                    if (checkHasDegreeAround(x, y, 2) && checkHasGasAround(x, y, 2)) {
                        setShadow(x, y, Styles.DIRTY_BRIGHT_BLACK);
                    }
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

    public static Color getColorDynamic(DynamicWorldObjects object) {
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
        var block = world.getBlock(x, y);
        return block != null && block.type != Type.GAS;
    }

    private static boolean checkHasGasAround(int x, int y, int radius) {
        return isNotGas(x - radius, y) &&
               isNotGas(x + radius, y) &&
               isNotGas(x, y - radius) &&
               isNotGas(x, y + radius) &&
               isNotGas(x, y);
    }

    private static boolean checkHasDegreeAround(int x, int y, int radius) {
        return getDegree(x - radius, y) > 0 &&
               getDegree(x + radius, y) > 0 &&
               getDegree(x, y + radius) > 0 &&
               getDegree(x, y - radius) > 0;
    }
}
