package core.World.Textures;

import core.Utils.SimpleColor;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.WorldGenerator;
import java.util.*;
import static core.Window.start;
import static core.World.StaticWorldObjects.StaticWorldObjects.getType;
import static core.World.WorldGenerator.*;

public class ShadowMap {
    private static SimpleColor[] shadows;
    private static ArrayList<SimpleColor> shadowsDynamic = new ArrayList<>(10);
    private static SimpleColor deletedColor = new SimpleColor(0, 0, 0, 0), deletedColorDynamic = new SimpleColor(0, 0, 0, 0);
    private static SimpleColor addedColor = new SimpleColor(0, 0, 0, 0), addedColorDynamic = new SimpleColor(0, 0, 0, 0);

    //TODO: rewrite generation n update

    public static SimpleColor getShadow(int x, int y) {
        return shadows[x + SizeX * y];
    }

    public static void setShadow(int x, int y, SimpleColor color) {
        shadows[x + SizeX * y] = color;
    }

    public static int getDegree(int x, int y) {
        int rgb = getShadow(x, y).getRed() + getShadow(x, y).getGreen() + getShadow(x, y).getBlue();
        return (int) Math.abs(Math.ceil(rgb / 198f - 4));
    }

    public static void generate() {
        shadows = new SimpleColor[(WorldGenerator.SizeX + 1) * (WorldGenerator.SizeY + 1)];
        Arrays.fill(shadows, new SimpleColor(255, 255, 255, 255));

        generateShadows();
    }

    private static void generateShadows() {
        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                if (checkHasGasAround(x, y, 1)) {
                    setShadow(x, y, new SimpleColor(165, 165, 165, 255));
                } else {
                    setShadow(x, y, new SimpleColor(255, 255, 255, 255));
                }
            }
        }

        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                if (checkHasGasAround(x, y, 1) && checkHasDegreeAround(x, y, 1)) {
                    setShadow(x, y, new SimpleColor(85, 85, 85, 255));
                }
            }
        }

        for (int x = 2; x < WorldGenerator.SizeX - 2; x++) {
            for (int y = 2; y < WorldGenerator.SizeY - 2; y++) {
                if (checkHasDegreeAround(x, y, 2) && checkHasGasAround(x, y, 2)) {
                    setShadow(x, y, new SimpleColor(5, 5, 5, 255));
                }
            }
        }
    }

    public static void update() {
        if (start) {
            for (int x = (int) (DynamicObjects.get(0).x / 16) - 20; x < DynamicObjects.get(0).x / 16 + 21; x++) {
                for (int y = (int) (DynamicObjects.get(0).y / 16) - 8; y < DynamicObjects.get(0).y / 16 + 16; y++) {
                    if (checkHasGasAround(x, y, 1)) {
                        setShadow(x, y, new SimpleColor(165, 165, 165, 255));
                    } else {
                        setShadow(x, y, new SimpleColor(255, 255, 255, 255));
                    }
                }
            }
            for (int x = (int) (DynamicObjects.get(0).x / 16) - 20; x < DynamicObjects.get(0).x / 16 + 21; x++) {
                for (int y = (int) (DynamicObjects.get(0).y / 16) - 8; y < DynamicObjects.get(0).y / 16 + 16; y++) {
                    if (checkHasGasAround(x, y, 1) && checkHasDegreeAround(x, y, 1)) {
                        setShadow(x, y, new SimpleColor(85, 85, 85, 255));
                    }
                }
            }
            for (int x = (int) (DynamicObjects.get(0).x / 16) - 20; x < DynamicObjects.get(0).x / 16 + 21; x++) {
                for (int y = (int) (DynamicObjects.get(0).y / 16) - 8; y < DynamicObjects.get(0).y / 16 + 16; y++) {
                    if (checkHasDegreeAround(x, y, 2) && checkHasGasAround(x, y, 2)) {
                        setShadow(x, y, new SimpleColor(5, 5, 5, 255));
                    }
                }
            }
        }
    }

//    public static int getBlockDepthVertical(int x, int y) {
//        int maxDepth = 4;
//        int depth = 0;
//
//        for (int i = y; i < SizeY; i++) {
//            if (depth >= maxDepth || getType(getObject(x, i + 1)) == StaticObjectsConst.Types.GAS) {
//                break;
//            }
//            depth++;
//        }
//        return depth;
//    }
//
//    private static int getBlockLightingVertical(int x, int y) {
//        int lighting = 255;
//
//        for (int i = y; i < SizeY; i++) {
//            if (getType(getObject(x, i)) != StaticObjectsConst.Types.GAS) {
//                lighting -= Math.abs(255 - StaticWorldObjects.getLightTransmission(getObject(x, i)));
//
//                if (lighting < 10) {
//                    return 10;
//                }
//            } else {
//                return 255;
//            }
//        }
//        return lighting;
//    }

    private static SimpleColor calculateColor(int lighting, SimpleColor originalColor) {
        int r = originalColor.getRed() - Math.abs(255 - lighting);
        int g = originalColor.getGreen() - Math.abs(255 - lighting);
        int b = originalColor.getBlue() - Math.abs(255 - lighting);

        return checkColor(new SimpleColor(r, g, b, originalColor.getAlpha()));
    }

    public static SimpleColor getColor(int x, int y) {
        int r = checkColor(getShadow(x, y).getRed() + addedColor.getRed() - deletedColor.getRed());
        int g = checkColor(getShadow(x, y).getGreen() + addedColor.getGreen() - deletedColor.getGreen());
        int b = checkColor(getShadow(x, y).getBlue() + addedColor.getBlue() - deletedColor.getBlue());
        int a = checkColor(getShadow(x, y).getAlpha() + addedColor.getAlpha() - deletedColor.getAlpha());

        return new SimpleColor(r, g, b, a);
    }

    public static SimpleColor getColorDynamic(int cell) {
        if (shadowsDynamic != null && cell < shadowsDynamic.size()) {
            int r = checkColor(shadowsDynamic.get(cell).getRed() + addedColorDynamic.getRed() - deletedColorDynamic.getRed());
            int g = checkColor(shadowsDynamic.get(cell).getGreen() + addedColorDynamic.getGreen() - deletedColorDynamic.getGreen());
            int b = checkColor(shadowsDynamic.get(cell).getBlue() + addedColorDynamic.getBlue() - deletedColorDynamic.getBlue());
            int a = checkColor(shadowsDynamic.get(cell).getAlpha() + addedColorDynamic.getAlpha() - deletedColorDynamic.getAlpha());

            return new SimpleColor(r, g, b, a);
        }
        shadowsDynamic.add(new SimpleColor(255, 255, 255, 255));

        return new SimpleColor(255, 255, 255, 255);
    }

    public static void addAllColor(SimpleColor color) {
        addedColor = checkColor(color);
    }

    public static void addAllColorDynamic(SimpleColor color) {
        addedColorDynamic = checkColor(color);
    }

    public static void deleteAllColor(SimpleColor color) {
        deletedColor = checkColor(color);
    }

    public static void deleteAllColorDynamic(SimpleColor color) {
        deletedColorDynamic = checkColor(color);
    }

    public static void setColorBrightness(SimpleColor color, int brightness) {
        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                if (getDegree(x, y) == brightness) {
                    int r = checkColor(getShadow(x, y).getRed() + color.getRed());
                    int g = checkColor(getShadow(x, y).getGreen() + color.getGreen());
                    int b = checkColor(getShadow(x, y).getBlue() + color.getBlue());
                    int a = checkColor(getShadow(x, y).getAlpha() + color.getAlpha());

                    setShadow(x, y, new SimpleColor(r, g, b, a));
                }
            }
        }
    }

    public static int checkColor(int SimpleColor) {
        return Math.min(Math.max(SimpleColor, 0), 255);
    }

    private static SimpleColor checkColor(SimpleColor color) {
        int r = checkColor(color.getRed());
        int g = checkColor(color.getGreen());
        int b = checkColor(color.getBlue());
        int a = checkColor(color.getAlpha());

        return new SimpleColor(r, g, b, a);
    }

    private static SimpleColor checkLightTransmission(SimpleColor color, int transmission) {
        int r = checkColor(color.getRed() + transmission);
        int g = checkColor(color.getGreen() + transmission);
        int b = checkColor(color.getBlue() + transmission);
        int a = checkColor(color.getAlpha() + transmission);

        return new SimpleColor(r, g, b, a);
    }

    private static boolean checkHasGasAround(int x, int y, int radius) {
        return getType(getObject(x - radius, y)) != StaticObjectsConst.Types.GAS && getType(getObject(x + radius, y)) != StaticObjectsConst.Types.GAS && getType(getObject(x, y - radius)) != StaticObjectsConst.Types.GAS && getType(getObject(x, y + radius)) != StaticObjectsConst.Types.GAS && getType(getObject(x, y)) != StaticObjectsConst.Types.GAS;
    }

    private static boolean checkHasDegreeAround(int x, int y, int radius) {
        return getDegree(x - radius, y) > 0 && getDegree(x + radius, y) > 0 && getDegree(x, y + radius) > 0 && getDegree(x, y - radius) > 0;
    }

    public static HashMap<String, Object> getAllData() {
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
        shadows = (SimpleColor[]) data.get("Shadows");
        shadowsDynamic = (ArrayList<SimpleColor>) data.get("ShadowsDynamic");
        deletedColor = (SimpleColor) data.get("DeletedColor");
        deletedColorDynamic = (SimpleColor) data.get("DeletedColorDynamic");
        addedColor = (SimpleColor) data.get("AddedColor");
        addedColorDynamic = (SimpleColor) data.get("AddedColorDynamic");
    }
}
