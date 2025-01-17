package core.World.Textures;

import core.Utils.SimpleColor;
import core.World.Creatures.DynamicWorldObjects;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.WorldGenerator;
import core.math.MathUtil;
import core.math.Point2i;

import java.util.Arrays;
import java.util.HashMap;

import static core.Window.start;
import static core.World.StaticWorldObjects.StaticWorldObjects.getType;
import static core.World.WorldGenerator.*;

public class ShadowMap {
    private static int[] shadows;
    private static HashMap<DynamicWorldObjects, SimpleColor> shadowsDynamic = new HashMap<>();
    private static SimpleColor deletedColor = SimpleColor.CLEAR, deletedColorDynamic = SimpleColor.CLEAR, addedColor = SimpleColor.CLEAR, addedColorDynamic = SimpleColor.CLEAR;

    // todo переписать генерацию и обновление теней

    public static SimpleColor getShadow(int x, int y) {
        if (x < 0 || y < 0 || x >= SizeX || y >= SizeY) {
            return SimpleColor.CLEAR;
        }
        return SimpleColor.toColor(shadows[x + SizeX * y]);
    }

    public static void setShadow(int x, int y, SimpleColor color) {
        if (x < 0 || y < 0 || x >= SizeX || y >= SizeY) {
            return;
        }
        shadows[x + SizeX * y] = color.getValueARGB();
    }

    public static int getDegree(int x, int y) {
        int rgb = getShadow(x, y).getRed() + getShadow(x, y).getGreen() + getShadow(x, y).getBlue();
        return (int) Math.abs(Math.ceil(rgb / 198f - 4));
    }

    public static void generate() {
        shadows = new int[WorldGenerator.SizeX * WorldGenerator.SizeY];
        Arrays.fill(shadows, SimpleColor.WHITE.getValueARGB());

        generateShadows();
    }

    private static void generateShadows() {
        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                if (checkHasGasAround(x, y, 1)) {
                    setShadow(x, y, SimpleColor.fromRGBA(165, 165, 165, 255));
                } else {
                    setShadow(x, y, SimpleColor.WHITE);
                }
            }
        }

        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                if (checkHasGasAround(x, y, 1) && checkHasDegreeAround(x, y, 1)) {
                    setShadow(x, y, SimpleColor.fromRGBA(85, 85, 85, 255));
                }
            }
        }

        for (int x = 2; x < WorldGenerator.SizeX - 2; x++) {
            for (int y = 2; y < WorldGenerator.SizeY - 2; y++) {
                if (checkHasDegreeAround(x, y, 2) && checkHasGasAround(x, y, 2)) {
                    setShadow(x, y, SimpleColor.DIRTY_BRIGHT_BLACK);
                }
            }
        }
    }

    public static void update() {
        if (start) {
            int xPos = (int) DynamicObjects.getFirst().getX();
            int yPos = (int) DynamicObjects.getFirst().getY();

            for (int x = xPos / TextureDrawing.blockSize - 20; x < xPos / TextureDrawing.blockSize + 21; x++) {
                for (int y = yPos / TextureDrawing.blockSize - 8; y < yPos / TextureDrawing.blockSize + 16; y++) {
                    if (checkHasGasAround(x, y, 1)) {
                        setShadow(x, y, SimpleColor.fromRGBA(165, 165, 165, 255));
                    } else {
                        setShadow(x, y, SimpleColor.WHITE);
                    }
                }
            }
            for (int x = xPos / TextureDrawing.blockSize - 20; x < xPos / TextureDrawing.blockSize + 21; x++) {
                for (int y = yPos / TextureDrawing.blockSize - 8; y < yPos / TextureDrawing.blockSize + 16; y++) {
                    if (checkHasGasAround(x, y, 1) && checkHasDegreeAround(x, y, 1)) {
                        setShadow(x, y, SimpleColor.fromRGBA(85, 85, 85, 255));
                    }
                }
            }
            for (int x = xPos / TextureDrawing.blockSize - 20; x < xPos / TextureDrawing.blockSize + 21; x++) {
                for (int y = yPos / TextureDrawing.blockSize - 8; y < yPos / TextureDrawing.blockSize + 16; y++) {
                    if (checkHasDegreeAround(x, y, 2) && checkHasGasAround(x, y, 2)) {
                        setShadow(x, y, SimpleColor.DIRTY_BRIGHT_BLACK);
                    }
                }
            }
        }
    }

    public static SimpleColor getColor(int x, int y) {
        int r = getShadow(x, y).getRed() + addedColor.getRed() - deletedColor.getRed();
        int g = getShadow(x, y).getGreen() + addedColor.getGreen() - deletedColor.getGreen();
        int b = getShadow(x, y).getBlue() + addedColor.getBlue() - deletedColor.getBlue();
        int a = getShadow(x, y).getAlpha() + addedColor.getAlpha() - deletedColor.getAlpha();

        return SimpleColor.fromRGBA(r, g, b, a);
    }

    public static SimpleColor getColorDynamic(DynamicWorldObjects object) {
        SimpleColor color = shadowsDynamic.getOrDefault(object, null);

        if (color == null) {
            shadowsDynamic.put(object, SimpleColor.WHITE);
            color = SimpleColor.WHITE;
        }

        int r = color.getRed() + addedColorDynamic.getRed() - deletedColorDynamic.getRed();
        int g = color.getGreen() + addedColorDynamic.getGreen() - deletedColorDynamic.getGreen();
        int b = color.getBlue() + addedColorDynamic.getBlue() - deletedColorDynamic.getBlue();
        int a = color.getAlpha() + addedColorDynamic.getAlpha() - deletedColorDynamic.getAlpha();

        return SimpleColor.fromRGBA(r, g, b, a);
    }

    public static void addAllColor(SimpleColor color) {
        addedColor = color;
    }

    public static void addAllColorDynamic(SimpleColor color) {
        addedColorDynamic = color;
    }

    public static void deleteAllColor(SimpleColor color) {
        deletedColor = color;
    }

    public static void deleteAllColorDynamic(SimpleColor color) {
        deletedColorDynamic = color;
    }

    private static boolean checkHasGasAround(int x, int y, int radius) {
        return getType(getObject(x - radius, y)) != StaticObjectsConst.Types.GAS && getType(getObject(x + radius, y)) != StaticObjectsConst.Types.GAS && getType(getObject(x, y - radius)) != StaticObjectsConst.Types.GAS && getType(getObject(x, y + radius)) != StaticObjectsConst.Types.GAS && getType(getObject(x, y)) != StaticObjectsConst.Types.GAS;
    }

    private static boolean checkHasDegreeAround(int x, int y, int radius) {
        return getDegree(x - radius, y) > 0 && getDegree(x + radius, y) > 0 && getDegree(x, y + radius) > 0 && getDegree(x, y - radius) > 0;
    }

    public static HashMap<String, Object> getShadowData() {
        HashMap<String, Object> data = new HashMap<>();

        data.put("Shadows", shadows);
        data.put("ShadowsDynamic", shadowsDynamic);
        data.put("DeletedColor", deletedColor);
        data.put("DeletedColorDynamic", deletedColorDynamic);
        data.put("AddedColor", addedColor);
        data.put("AddedColorDynamic", addedColorDynamic);

        return data;
    }

    public static void setAllData(HashMap<String, Object> data) {
        shadows = (int[]) data.get("Shadows");
        shadowsDynamic = (HashMap<DynamicWorldObjects, SimpleColor>) data.get("ShadowsDynamic");
        deletedColor = (SimpleColor) data.get("DeletedColor");
        deletedColorDynamic = (SimpleColor) data.get("DeletedColorDynamic");
        addedColor = (SimpleColor) data.get("AddedColor");
        addedColorDynamic = (SimpleColor) data.get("AddedColorDynamic");
    }
}
