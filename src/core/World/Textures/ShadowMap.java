package core.World.Textures;

import core.World.WorldGenerator;
import java.awt.*;
import java.util.Arrays;
import static core.World.WorldGenerator.StaticObjects;

public class ShadowMap {
    public static Color[][] shadows;
    private static Color addedColor = new Color(0, 0, 0, 0);

    public static void generate() {
        shadows = new Color[WorldGenerator.SizeX][WorldGenerator.SizeY];
        for (Color[] shadow : shadows) {
            Arrays.fill(shadow, new Color(255, 255, 255, 255));
        }
    }

    public static void update() {
        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                if (!StaticObjects[x - 1][y].gas && !StaticObjects[x + 1][y].gas && !StaticObjects[x][y - 1].gas && !StaticObjects[x][y + 1].gas) {
                    shadows[x][y] = new Color(140, 140, 140, 255);
                } else {
                    shadows[x][y] = new Color(255, 255, 255, 255);
                }
            }
        }

        for (int x = 1; x < WorldGenerator.SizeX - 1; x++) {
            for (int y = 1; y < WorldGenerator.SizeY - 1; y++) {
                boolean hasGas = !StaticObjects[x - 1][y].gas && !StaticObjects[x + 1][y].gas && !StaticObjects[x][y - 1].gas && !StaticObjects[x][y + 1].gas && !StaticObjects[x][y].gas;
                boolean hasColor = shadows[x - 1][y].getRed() < 255 && shadows[x + 1][y].getRed() < 255 && shadows[x][y + 1].getRed() < 255 && shadows[x][y - 1].getRed() < 255;

                if (hasColor && hasGas) {
                    shadows[x][y] = new Color(10, 10, 10, 255);
                }
            }
        }
    }

    public static Color getColor(int x, int y) {
        int r = Math.min(Math.max(shadows[x][y].getRed() + addedColor.getRed(), 0), 255);
        int g = Math.min(Math.max(shadows[x][y].getGreen() + addedColor.getGreen(), 0), 255);
        int b = Math.min(Math.max(shadows[x][y].getBlue() + addedColor.getBlue(), 0), 255);
        int a = Math.min(Math.max(shadows[x][y].getAlpha() + addedColor.getAlpha(), 0), 255);

        return new Color(r, g, b, a);
    }

    public static void addColor(Color color) {
        int r = Math.min(Math.max(addedColor.getRed(), 0), 255);
        int g = Math.min(Math.max(addedColor.getGreen(), 0), 255);
        int b = Math.min(Math.max(addedColor.getBlue(), 0), 255);
        int a = Math.min(Math.max(addedColor.getAlpha(), 0), 255);

        addedColor = new Color(r, g, b, a);
    }

    public static void addColor(Color color, int x, int y) {
        int r = Math.min(Math.max(shadows[x][y].getRed() + color.getRed(), 0), 255);
        int g = Math.min(Math.max(shadows[x][y].getGreen() + color.getGreen(), 0), 255);
        int b = Math.min(Math.max(shadows[x][y].getBlue() + color.getBlue(), 0), 255);
        int a = Math.min(Math.max(shadows[x][y].getAlpha() + color.getAlpha(), 0), 255);

        shadows[x][y] = new Color(r, g, b, a);
    }
}
