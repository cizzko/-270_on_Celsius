package core.World.Weather;

import java.awt.*;
import static core.Window.*;
import static core.World.Textures.TextureDrawing.*;
import static core.World.WorldGenerator.*;
import static core.World.WorldGenerator.DynamicObjects;

public class Sun {
    public static float currentTime = (float) (Math.random() * 2359), x, y = -500 * (1 -  (currentTime - 2359) / (1 - 2359)) + 1500 * (currentTime - 2359) / (1 - 2359) + (SizeY / 2f * 16 - 700);
    public static boolean visible = false;
    private static final int startSunset = 1000, endSunset = 1300;
    private static long lastTime = System.currentTimeMillis();

    public static void createSun() {
        visible = true;
    }

    public static void updateSun() {
        if (visible) {
            if (System.currentTimeMillis() - lastTime >= 50) {
                lastTime = System.currentTimeMillis();
                currentTime++;

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
            }
            final int minGreen = 85;
            final int maxGreen = 255;

            double ratio = (double) (maxGreen - minGreen) / (2359 - minGreen);
            int green = (int) (maxGreen - (currentTime * ratio));

            updateGradient(green, maxGreen);
            drawTexture(defPath + "\\src\\assets\\World\\other\\sun.png", 580, y, 1, new Color(255, green, 40, 220), true);
        }
    }

    private static void updateGradient(int green, int maxGreen) {
        double alpha = 0.5 * (1 + Math.sin(Math.PI * (currentTime - startSunset) / (endSunset - startSunset)));
        int aGradient = (int) (255 * alpha);

        drawTexture(defPath + "\\src\\assets\\World\\other\\interpolatedSunsnet.png", 0, 0, 1, new Color(aGradient, 0, 20, aGradient), true);
    }
}
