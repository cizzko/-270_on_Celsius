package core.World.Textures;

import core.World.Textures.StaticWorldObjects.StaticObjectsConst;
import core.World.WorldGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import static core.World.WorldGenerator.*;

public class ShadowMap {
    private static SimpleColor[][] shadows;
    private static ArrayList<SimpleColor> shadowsDynamic = new ArrayList<>(10);
    public static int[][] colorDegree;
    private static SimpleColor deletedColor = new SimpleColor(0, 0, 0, 0), deletedColorDynamic = new SimpleColor(0, 0, 0, 0);
    private static SimpleColor addedSimpleColor = new SimpleColor(0, 0, 0, 0), addedSimpleColorDynamic = new SimpleColor(0, 0, 0, 0);

    //TODO: рекомендуется переписать генерацию и апдейт

    public static void generate() {
        shadows = new SimpleColor[WorldGenerator.SizeX][WorldGenerator.SizeY];
        colorDegree = new int[WorldGenerator.SizeX][WorldGenerator.SizeY];

        for (SimpleColor[] shadow : shadows) {
            Arrays.fill(shadow, new SimpleColor(255, 255, 255, 255));
        }
        for (int[] SimpleColor : colorDegree) {
            Arrays.fill(SimpleColor, 0);
        }
        generateShadows();
    }

    private static void generateShadows() {
        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                if (getObject(x - 1, y).getType() != StaticObjectsConst.Types.GAS && getObject(x + 1, y).getType() != StaticObjectsConst.Types.GAS && getObject(x, y - 1).getType() != StaticObjectsConst.Types.GAS && getObject(x, y + 1).getType() != StaticObjectsConst.Types.GAS) {
                    colorDegree[x][y] = 1;
                    shadows[x][y] = new SimpleColor(140, 140, 140, 255);
                } else {
                    colorDegree[x][y] = 0;
                    shadows[x][y] = new SimpleColor(255, 255, 255, 255);
                }
            }
        }

        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                boolean hasGas = getObject(x - 1, y).getType() != StaticObjectsConst.Types.GAS && getObject(x + 1, y).getType() != StaticObjectsConst.Types.GAS && getObject(x, y - 1).getType() != StaticObjectsConst.Types.GAS && getObject(x, y + 1).getType() != StaticObjectsConst.Types.GAS && getObject(x, y).getType() != StaticObjectsConst.Types.GAS;
                boolean hasSimpleColor = colorDegree[x - 1][y] > 0 && colorDegree[x + 1][y] > 0 && colorDegree[x][y + 1] > 0 && colorDegree[x][y - 1] > 0;

                if (hasSimpleColor && hasGas) {
                    colorDegree[x][y] = 2;
                    shadows[x][y] = new SimpleColor(80, 80, 80, 255);
                }
            }
        }

        for (int x = 2; x < WorldGenerator.SizeX - 2; x++) {
            for (int y = 2; y < WorldGenerator.SizeY - 2; y++) {
                boolean hasGas = getObject(x - 2, y).getType() != StaticObjectsConst.Types.GAS && getObject(x + 2, y).getType() != StaticObjectsConst.Types.GAS && getObject(x, y - 2).getType() != StaticObjectsConst.Types.GAS && getObject(x, y + 2).getType() != StaticObjectsConst.Types.GAS && getObject(x, y).getType() != StaticObjectsConst.Types.GAS;
                boolean hasSimpleColor = colorDegree[x - 2][y] > 0 && colorDegree[x + 2][y] > 0 && colorDegree[x][y + 2] > 0 && colorDegree[x][y - 2] > 0;

                if (hasSimpleColor && hasGas) {
                    colorDegree[x][y] = 3;
                    shadows[x][y] = new SimpleColor(10, 10, 10, 255);
                }
            }
        }
    }

    public static void update() {
        for (int x = (int) (DynamicObjects.get(0).x / 16) - 20; x < DynamicObjects.get(0).x / 16 + 21; x++) {
            for (int y = (int) (DynamicObjects.get(0).y / 16) - 8; y < DynamicObjects.get(0).y / 16 + 16; y++) {
                if (x < 5 || y < 5 || x > SizeX - 5 || y > SizeY - 5 || getObject(x, y) == null) {
                    continue;
                }

                if (getObject(x - 1, y).getType() != StaticObjectsConst.Types.GAS && getObject(x + 1, y).getType() != StaticObjectsConst.Types.GAS && getObject(x, y - 1).getType() != StaticObjectsConst.Types.GAS && getObject(x, y + 1).getType() != StaticObjectsConst.Types.GAS) {
                    colorDegree[x][y] = 1;
                    shadows[x][y] = new SimpleColor(140, 140, 140, 255);
                } else {
                    colorDegree[x][y] = 0;
                    shadows[x][y] = new SimpleColor(255, 255, 255, 255);
                }

                boolean hasGas = getObject(x - 1, y).getType() != StaticObjectsConst.Types.GAS && getObject(x + 1, y).getType() != StaticObjectsConst.Types.GAS && getObject(x, y - 1).getType() != StaticObjectsConst.Types.GAS && getObject(x, y + 1).getType() != StaticObjectsConst.Types.GAS && getObject(x, y).getType() != StaticObjectsConst.Types.GAS;
                boolean hasSimpleColor = colorDegree[x - 1][y] > 0 && colorDegree[x + 1][y] > 0 && colorDegree[x][y + 1] > 0 && colorDegree[x][y - 1] > 0;

                if (hasSimpleColor && hasGas) {
                    colorDegree[x][y] = 2;
                    shadows[x][y] = new SimpleColor(80, 80, 80, 255);
                }

                hasGas = getObject(x - 2, y).getType() != StaticObjectsConst.Types.GAS && getObject(x + 2, y).getType() != StaticObjectsConst.Types.GAS && getObject(x, y - 2).getType() != StaticObjectsConst.Types.GAS && getObject(x, y + 2).getType() != StaticObjectsConst.Types.GAS && getObject(x, y).getType() != StaticObjectsConst.Types.GAS;
                hasSimpleColor = colorDegree[x - 2][y] > 0 && colorDegree[x + 2][y] > 0 && colorDegree[x][y + 2] > 0 && colorDegree[x][y - 2] > 0;

                if (hasSimpleColor && hasGas) {
                    colorDegree[x][y] = 3;
                    shadows[x][y] = new SimpleColor(10, 10, 10, 255);
                }
            }
        }
    }

    public static SimpleColor getSimpleColor(int x, int y) {
        int r = checkColor(shadows[x][y].getRed() + addedSimpleColor.getRed() - deletedColor.getRed());
        int g = checkColor(shadows[x][y].getGreen() + addedSimpleColor.getGreen() - deletedColor.getGreen());
        int b = checkColor(shadows[x][y].getBlue() + addedSimpleColor.getBlue() - deletedColor.getBlue());
        int a = checkColor(shadows[x][y].getAlpha() + addedSimpleColor.getAlpha() - deletedColor.getAlpha());

        return new SimpleColor(r, g, b, a);
    }

    public static SimpleColor getSimpleColorDynamic(int cell) {
        if (shadowsDynamic != null && cell < shadowsDynamic.size()) {
            int r = checkColor(shadowsDynamic.get(cell).getRed() + addedSimpleColorDynamic.getRed() - deletedColorDynamic.getRed());
            int g = checkColor(shadowsDynamic.get(cell).getGreen() + addedSimpleColorDynamic.getGreen() - deletedColorDynamic.getGreen());
            int b = checkColor(shadowsDynamic.get(cell).getBlue() + addedSimpleColorDynamic.getBlue() - deletedColorDynamic.getBlue());
            int a = checkColor(shadowsDynamic.get(cell).getAlpha() + addedSimpleColorDynamic.getAlpha() - deletedColorDynamic.getAlpha());

            return new SimpleColor(r, g, b, a);
        }
        shadowsDynamic.add(new SimpleColor(255, 255, 255, 255));

        return new SimpleColor(255, 255, 255, 255);
    }

    public static void addAllSimpleColor(SimpleColor color) {
        addedSimpleColor = checkColor(color);
    }

    public static void addAllSimpleColorDynamic(SimpleColor color) {
        addedSimpleColorDynamic = checkColor(color);
    }

    public static void deleteAllSimpleColor(SimpleColor color) {
        deletedColor = checkColor(color);
    }

    public static void deleteAllSimpleColorDynamic(SimpleColor color) {
        deletedColorDynamic = checkColor(color);
    }

    public static void setColorBrightness(SimpleColor color, int brightness) {
        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                if (colorDegree[x][y] == brightness) {
                    int r = checkColor(shadows[x][y].getRed() + color.getRed());
                    int g = checkColor(shadows[x][y].getGreen() + color.getGreen());
                    int b = checkColor(shadows[x][y].getBlue() + color.getBlue());
                    int a = checkColor(shadows[x][y].getAlpha() + color.getAlpha());

                    shadows[x][y] = new SimpleColor(r, g, b, a);
                }
            }
        }
    }

    public static void setColor(int cellX, int cellY, SimpleColor color) {
        shadows[cellX][cellY] = checkColor(color);
    }

    private static int checkColor(int SimpleColor) {
        return Math.min(Math.max(SimpleColor, 0), 255);
    }

    private static SimpleColor checkColor(SimpleColor color) {
        int r = checkColor(color.getRed());
        int g = checkColor(color.getGreen());
        int b = checkColor(color.getBlue());
        int a = checkColor(color.getAlpha());

        return new SimpleColor(r, g, b, a);
    }

    public static HashMap<String, Object> getAllData() {
        HashMap<String, Object> data = new HashMap<>();

        data.put("Shadows", shadows);
        data.put("ShadowsDynamic", shadowsDynamic);
        data.put("colorDegree", colorDegree);
        data.put("DeletedSimpleColor", deletedColor);
        data.put("DeletedSimpleColorDynamic", deletedColorDynamic);
        data.put("AddedSimpleColor", addedSimpleColor);
        data.put("AddedSimpleColorDynamic", addedSimpleColorDynamic);

        return data;
    }

    public static void setAllData(HashMap<String, Object> data) {
        shadows = (SimpleColor[][]) data.get("Shadows");
        shadowsDynamic = (ArrayList<SimpleColor>) data.get("ShadowsDynamic");
        colorDegree = (int[][]) data.get("colorDegree");
        deletedColor = (SimpleColor) data.get("DeletedSimpleColor");
        deletedColorDynamic = (SimpleColor) data.get("DeletedSimpleColorDynamic");
        addedSimpleColor = (SimpleColor) data.get("AddedSimpleColor");
        addedSimpleColorDynamic = (SimpleColor) data.get("AddedSimpleColorDynamic");
    }
}
