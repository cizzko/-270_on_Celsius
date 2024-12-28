package core.World.Background.DynamicBackground.Weather;

import core.Utils.SimpleColor;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.Textures.ShadowMap;
import core.World.WorldGenerator;

import static core.EventHandling.Logging.Config.getFromConfig;
import static core.Global.assets;
import static core.Global.batch;
import static core.World.WorldGenerator.DynamicObjects;

public class Sun {
    public static final int startSunset = 140, endSunset = 1200, endDay = 1350, startDay = 1900, startYSun = 1400, endYSun = -1670;
    public static float currentTime = (float) (Math.random() * 2400), x, y = endYSun * (1 -  (currentTime - 2400) / (1 - 2400)) + startYSun * (currentTime - 2400) / (1 - 2400);
    public static boolean visible = false;
    private static long lastTime = System.currentTimeMillis();

    public static void createSun() {
        visible = true;
    }

    public static void updateSun() {
        if (visible) {
            if (System.currentTimeMillis() - lastTime >= 900) {
                lastTime = System.currentTimeMillis();
                currentTime++;

                if (currentTime > 2400 || currentTime < 0) { // 2400 - 23:59
                    WorldGenerator.dayCount++;
                    currentTime = 0;
                }
                x = DynamicObjects.getFirst().getX();

                if (currentTime >= 2400 || currentTime < 1) {
                    y = endYSun;
                } else {
                    double t = (currentTime - 2400) / (1 - 2400);
                    y = (float) (endYSun * (1 - t) + startYSun * t);
                }
                TemperatureMap.update();
            }
            int minGreen = 85;
            int maxGreen = 255;

            double ratio = (double) (maxGreen - minGreen) / (2400 - minGreen);
            int green = (int) (maxGreen - (currentTime * ratio));

            updateNightBackground();
            updateGradient();

            batch.color(SimpleColor.fromRGBA(255, green, 40, 220));
            batch.draw(assets.getTextureByPath(assets.assetsDir("World/Sun/sun.png")), 580, y);
            batch.resetColor();
        }
    }

    private static void updateGradient() {
        double alpha = 0;

        if (currentTime >= startSunset && currentTime <= endSunset) {
            alpha = Lerp(0, 1, (currentTime - startSunset) / (endSunset - startSunset));
        } else if (currentTime > endSunset) {
            alpha = Lerp(1, 0, (currentTime - endSunset) / (endSunset - startSunset));
        }
        int aGradient = (int) (250 * alpha);
        aGradient = Math.max(0, Math.min(250, aGradient));

        String sunsetType = getFromConfig("InterpolateSunset").equals("true") ? "" : "non";
        batch.color(SimpleColor.fromRGBA(aGradient, 0, 20, aGradient));
        batch.draw(assets.getTextureByPath(assets.assetsDir("World/Sun/" + sunsetType + "InterpolatedSunset.png")));
        batch.resetColor();
    }

    private static void updateNightBackground() {
        double alpha = 0;

        if (currentTime >= endDay && currentTime <= startDay) {
            alpha = Lerp(0, 1, (currentTime - endDay) / (startDay - endDay));
        } else if (currentTime > startDay) {
            alpha = Lerp(1, 0, (currentTime - startDay) / (startDay - endDay));
        }
        int aGradient = (int) (255 * alpha);
        int deleteGradient = Math.max(0, Math.min(150, aGradient));
        int backGradient = Math.max(0, Math.min(255, aGradient));

        ShadowMap.deleteAllColor(SimpleColor.fromRGBA(deleteGradient, deleteGradient, deleteGradient, 0));
        ShadowMap.deleteAllColorDynamic(SimpleColor.fromRGBA(deleteGradient, deleteGradient, deleteGradient, 0));

        batch.color(SimpleColor.fromRGBA(255, 255, 255, backGradient));
        batch.draw(assets.getTextureByPath(assets.assetsDir("World/Sky/skyBackground0.png")));
        batch.resetColor();
    }

    private static double Lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
