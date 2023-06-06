package core.World.Weather;

import core.EventHandling.MouseScrollCallback;
import java.awt.*;

import static core.Window.*;
import static core.World.Textures.TextureDrawing.*;
import static core.World.WorldGenerator.*;
import static core.World.WorldGenerator.DynamicObjects;

public class Sun {
    public static float currentTime = (float) (Math.random() * 2359), x, y = SizeY / 2f;
    public static boolean visible = true;
    private static int startPlayerX = 320;

    public static void createSun() {
        visible = true;
    }

    public static void updateSun() {;
        if (currentTime > 2359 || currentTime < 0) { // 2359 - 23:59
            currentTime = 0;
        }
        if (DynamicObjects[0].isPlayer) {
            x = DynamicObjects[0].x;

            if (currentTime >= 2359 || currentTime < 1) {
                y = -500;
            } else {
                double t = (currentTime - 2359) / (1 - 2359);
                y = (float) (-500 * (1 - t) + 1500 * t) + (SizeY / 2f * 16 - 700);
            }
        }

        // больше время - краснее солнце
        final int minGreen = 85;
        final int maxGreen = 255;

        double ratio = (double) (maxGreen - minGreen) / (2359 - minGreen);
        int green = (int) (maxGreen - (currentTime * ratio));

        drawGradient(green, maxGreen);
        drawTexture(defPath + "\\src\\assets\\World\\other\\sun.png", (int) ((DynamicObjects[0].x / 3 + width / 2f - 32) - (DynamicObjects[0].x - startPlayerX)), (int) y, 1, new Color(255, green, 40, 220));
    }

    private static void drawGradient(int green, int maxGreen) {
        int segments;
        int startSunset = 950;
        int endSunset = 1300;
        int maxSegments = 1000;

        if (currentTime < startSunset) {
            segments = 201;
        } else if (currentTime >= startSunset && currentTime < endSunset) {
            segments = 201 + (int) ((currentTime - startSunset) * (maxSegments - 201) / (endSunset - startSunset));
        } else {
            segments = maxSegments - (int) ((currentTime - endSunset) * (maxSegments - 201) / (2359 - endSunset));
        }

        Color[] segmentColors = new Color[segments];

        for (int i = 0; i < segments; i++) {
            int greenNdBlue = Math.round(green + (maxGreen - green) * (float) i / (segments - 1));
            segmentColors[i] = new Color(255, greenNdBlue, Math.min(greenNdBlue, Math.max(255, greenNdBlue + i)), Math.min(254, segments - i));
        }

        for (int i = 0; i < segments; i++) {
            drawRectangle((int) (-DynamicObjects[0].x * 3 + width / 2f - 32), (int) (SizeY / 2f * 16 + (SizeY / 2f * 16 - DynamicObjects[0].y) + i) - 305, SizeX * 16, height / segments, segmentColors[i]);
        }
    }
}
