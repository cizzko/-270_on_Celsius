package core.World.Weather;

import core.GameObject;
import core.Load;
import core.util.Color;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.Textures.ShadowMap;
import core.g2d.Texture;

import static core.EventHandling.Logging.Config.getFromConfig;
import static core.Global.*;
import static core.World.WorldGenerator.DynamicObjects;

public class Sun extends GameObject {
    public static final int startSunset = 140, endSunset = 1200, endDay = 1350, startDay = 1900, startYSun = 1400, endYSun = -1670;

    private final Color skyColor = new Color();
    private final Color sunColor = new Color();
    private final Color sunsetColor = new Color();

    private long lastTime = System.currentTimeMillis();

    public float currentTime = (float) (Math.random() * 2400);
    public float x, y = endYSun * (1 -  (currentTime - 2400) / (1 - 2400)) + startYSun * (currentTime - 2400) / (1 - 2400);

    @Load("World/Sky/skyBackground0.png")
    protected Texture skyBackgroundTex;
    @Load
    protected Texture sunsetTex;
    @Load("World/Sun/sun.png")
    protected Texture sunTex;

    protected String sunsetTexName() {
        String sunsetType = getFromConfig("InterpolateSunset").equals("true") ? "" : "non";
        return "World/Sun/" + sunsetType + "InterpolatedSunset.png";
    }

    @Override
    public void update() {
        if (System.currentTimeMillis() - lastTime >= 900) {
            lastTime = System.currentTimeMillis();
            currentTime++;

            if (currentTime > 2400 || currentTime < 0) { // 2400 - 23:59
                world.dayCount++;
                currentTime = 0;
            }
            x = DynamicObjects.getFirst().getX();

            if (currentTime >= 2400 || currentTime < 1) {
                y = endYSun;
            } else {
                double t = (currentTime - 2400) / (1 - 2400);
                y = (float) (endYSun * (1 - t) + startYSun * t);
            }
            TemperatureMap.update(this);
        }
        final int minGreen = 85;
        final int maxGreen = 255;

        double ratio = (double) (maxGreen - minGreen) / (2400 - minGreen);
        int green = (int) (maxGreen - (currentTime * ratio));

        updateNightBackground();
        updateGradient();

        sunColor.set(255, green, 40, 220);
    }

    private void updateGradient() {
        double alpha = 0;

        if (currentTime >= startSunset && currentTime <= endSunset) {
            alpha = Lerp(0, 1, (currentTime - startSunset) / (endSunset - startSunset));
        } else if (currentTime > endSunset) {
            alpha = Lerp(1, 0, (currentTime - endSunset) / (endSunset - startSunset));
        }
        int aGradient = (int) (250 * alpha);
        aGradient = Math.max(0, Math.min(250, aGradient));

        sunsetColor.set(aGradient, 0, 20, aGradient);
    }

    private void updateNightBackground() {
        double alpha = 0;

        if (currentTime >= endDay && currentTime <= startDay) {
            alpha = Lerp(0, 1, (currentTime - endDay) / (startDay - endDay));
        } else if (currentTime > startDay) {
            alpha = Lerp(1, 0, (currentTime - startDay) / (startDay - endDay));
        }
        int aGradient = (int) (255 * alpha);
        int deleteGradient = Math.max(0, Math.min(150, aGradient));
        int backGradient = Math.max(0, Math.min(255, aGradient));

        ShadowMap.deleteAllColor(Color.fromRgba8888(deleteGradient, deleteGradient, deleteGradient, 0));
        ShadowMap.deleteAllColorDynamic(Color.fromRgba8888(deleteGradient, deleteGradient, deleteGradient, 0));

        skyColor.set(255, 255, 255, backGradient);
    }

    @Override
    public void draw() {
        if (skyColor.a() > 0) {
            batch.draw(skyBackgroundTex, skyColor);
        }
        if (sunsetColor.a() > 0) {
            batch.draw(sunsetTex, sunsetColor);
        }
        batch.draw(sunTex, sunColor, 580, y);
    }

    private static double Lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
