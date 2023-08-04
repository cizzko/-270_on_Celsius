package core.World.Textures;

import core.World.WorldGenerator;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import static core.World.WorldGenerator.*;

public class ShadowMap {
    private static Color[][] shadows;
    private static Color[] shadowsDynamic;
    public static int[][] colorDegree;
    private static Color deletedColor = new Color(0, 0, 0, 0), deletedColorDynamic = new Color(0, 0, 0, 0);
    private static Color addedColor = new Color(0, 0, 0, 0), addedColorDynamic = new Color(0, 0, 0, 0);

    //TODO: рекомендуется переписать генерацию и апдейт

    public static void generate() {
        shadows = new Color[WorldGenerator.SizeX][WorldGenerator.SizeY];
        colorDegree = new int[WorldGenerator.SizeX][WorldGenerator.SizeY];
        shadowsDynamic = new Color[WorldGenerator.DynamicObjects.length];

        for (Color[] shadow : shadows) {
            Arrays.fill(shadow, new Color(255, 255, 255, 255));
        }
        for (int[] color : colorDegree) {
            Arrays.fill(color, 0);
        }
        Arrays.fill(shadowsDynamic, new Color(255, 255, 255, 255));
        generateShadows();
    }

    private static void generateShadows() {
        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                if (!StaticObjects[x - 1][y].gas && !StaticObjects[x + 1][y].gas && !StaticObjects[x][y - 1].gas && !StaticObjects[x][y + 1].gas) {
                    colorDegree[x][y] = 1;
                    shadows[x][y] = new Color(140, 140, 140, 255);
                } else {
                    colorDegree[x][y] = 0;
                    shadows[x][y] = new Color(255, 255, 255, 255);
                }
            }
        }

        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                boolean hasGas = !StaticObjects[x - 1][y].gas && !StaticObjects[x + 1][y].gas && !StaticObjects[x][y - 1].gas && !StaticObjects[x][y + 1].gas && !StaticObjects[x][y].gas;
                boolean hasColor = colorDegree[x - 1][y] > 0 && colorDegree[x + 1][y] > 0 && colorDegree[x][y + 1] > 0 && colorDegree[x][y - 1] > 0;

                if (hasColor && hasGas) {
                    colorDegree[x][y] = 2;
                    shadows[x][y] = new Color(80, 80, 80, 255);
                }
            }
        }

        for (int x = 2; x < WorldGenerator.SizeX - 2; x++) {
            for (int y = 2; y < WorldGenerator.SizeY - 2; y++) {
                boolean hasGas = !StaticObjects[x - 2][y].gas && !StaticObjects[x + 2][y].gas && !StaticObjects[x][y - 2].gas && !StaticObjects[x][y + 2].gas && !StaticObjects[x][y].gas;
                boolean hasColor = colorDegree[x - 2][y] > 0 && colorDegree[x + 2][y] > 0 && colorDegree[x][y + 2] > 0 && colorDegree[x][y - 2] > 0;

                if (hasColor && hasGas) {
                    colorDegree[x][y] = 3;
                    shadows[x][y] = new Color(10, 10, 10, 255);
                }
            }
        }
    }

    public static void update() {
        for (int x = (int) (DynamicObjects[0].x / 16) - 20; x < DynamicObjects[0].x / 16 + 21; x++) {
            for (int y = (int) (DynamicObjects[0].y / 16) - 8; y < DynamicObjects[0].y / 16 + 16; y++) {
                if (x < 2 || y < 2 || x > StaticObjects.length - 2 || y > StaticObjects.length - 2 || StaticObjects[x][y] == null) {
                    continue;
                }

                if (!StaticObjects[x - 1][y].gas && !StaticObjects[x + 1][y].gas && !StaticObjects[x][y - 1].gas && !StaticObjects[x][y + 1].gas) {
                    colorDegree[x][y] = 1;
                    shadows[x][y] = new Color(140, 140, 140, 255);
                } else {
                    colorDegree[x][y] = 0;
                    shadows[x][y] = new Color(255, 255, 255, 255);
                }

                boolean hasGas = !StaticObjects[x - 1][y].gas && !StaticObjects[x + 1][y].gas && !StaticObjects[x][y - 1].gas && !StaticObjects[x][y + 1].gas && !StaticObjects[x][y].gas;
                boolean hasColor = colorDegree[x - 1][y] > 0 && colorDegree[x + 1][y] > 0 && colorDegree[x][y + 1] > 0 && colorDegree[x][y - 1] > 0;

                if (hasColor && hasGas) {
                    colorDegree[x][y] = 2;
                    shadows[x][y] = new Color(80, 80, 80, 255);
                }

                hasGas = !StaticObjects[x - 2][y].gas && !StaticObjects[x + 2][y].gas && !StaticObjects[x][y - 2].gas && !StaticObjects[x][y + 2].gas && !StaticObjects[x][y].gas;
                hasColor = colorDegree[x - 2][y] > 0 && colorDegree[x + 2][y] > 0 && colorDegree[x][y + 2] > 0 && colorDegree[x][y - 2] > 0;

                if (hasColor && hasGas) {
                    colorDegree[x][y] = 3;
                    shadows[x][y] = new Color(10, 10, 10, 255);
                }
            }
        }
    }

    public static Color getColor(int x, int y) {
        int r = checkColor(shadows[x][y].getRed() + addedColor.getRed() - deletedColor.getRed());
        int g = checkColor(shadows[x][y].getGreen() + addedColor.getGreen() - deletedColor.getGreen());
        int b = checkColor(shadows[x][y].getBlue() + addedColor.getBlue() - deletedColor.getBlue());
        int a = checkColor(shadows[x][y].getAlpha() + addedColor.getAlpha() - deletedColor.getAlpha());

        return new Color(r, g, b, a);
    }

    public static Color getColorDynamic(int cell) {
        int r = checkColor(shadowsDynamic[cell].getRed() + addedColorDynamic.getRed() - deletedColorDynamic.getRed());
        int g = checkColor(shadowsDynamic[cell].getGreen() + addedColorDynamic.getGreen() - deletedColorDynamic.getGreen());
        int b = checkColor(shadowsDynamic[cell].getBlue() + addedColorDynamic.getBlue() - deletedColorDynamic.getBlue());
        int a = checkColor(shadowsDynamic[cell].getAlpha() + addedColorDynamic.getAlpha() - deletedColorDynamic.getAlpha());

        return new Color(r, g, b, a);
    }

    public static void addAllColor(Color color) {
        addedColor = checkColor(color);
    }

    public static void addAllColorDynamic(Color color) {
        addedColorDynamic = checkColor(color);
    }

    public static void deleteAllColor(Color color) {
        deletedColor = checkColor(color);
    }

    public static void deleteAllColorDynamic(Color color) {
        deletedColorDynamic = checkColor(color);
    }

    public static void setColorBrightness(Color color, int brightness) {
        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                if (colorDegree[x][y] == brightness) {
                    int r = checkColor(shadows[x][y].getRed() + color.getRed());
                    int g = checkColor(shadows[x][y].getGreen() + color.getGreen());
                    int b = checkColor(shadows[x][y].getBlue() + color.getBlue());
                    int a = checkColor(shadows[x][y].getAlpha() + color.getAlpha());

                    shadows[x][y] = new Color(r, g, b, a);
                }
            }
        }
    }

    public static void setColor(int cellX, int cellY, Color color) {
        shadows[cellX][cellY] = checkColor(color);
    }

    private static int checkColor(int color) {
        return Math.min(Math.max(color, 0), 255);
    }

    private static Color checkColor(Color color) {
        int r = checkColor(color.getRed());
        int g = checkColor(color.getGreen());
        int b = checkColor(color.getBlue());
        int a = checkColor(color.getAlpha());

        return new Color(r, g, b, a);
    }

    public static HashMap<String, Object> getAllData() {
        HashMap<String, Object> data = new HashMap<>();

        data.put("Shadows", shadows);
        data.put("ShadowsDynamic", shadowsDynamic);
        data.put("ColorDegree", colorDegree);
        data.put("DeletedColor", deletedColor);
        data.put("DeletedColorDynamic", deletedColorDynamic);
        data.put("AddedColor", addedColor);
        data.put("AddedColorDynamic", addedColorDynamic);

        return data;
    }

    public static void setAllData(HashMap<String, Object> data) {
        shadows = (Color[][]) data.get("Shadows");
        shadowsDynamic = (Color[]) data.get("ShadowsDynamic");
        colorDegree = (int[][]) data.get("ColorDegree");
        deletedColor = (Color) data.get("DeletedColor");
        deletedColorDynamic = (Color) data.get("DeletedColorDynamic");
        addedColor = (Color) data.get("AddedColor");
        addedColorDynamic = (Color) data.get("AddedColorDynamic");
    }
}
