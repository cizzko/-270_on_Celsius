package core.World.Weather;

import core.GameObject;
import core.Load;
import core.World.Creatures.Physics;
import core.World.Textures.TextureDrawing;
import core.util.Color;
import core.g2d.Texture;

import static core.EventHandling.Logging.Config.getFromConfig;
import static core.Global.*;
import static core.World.WorldGenerator.WorldGenerator.DynamicObjects;

public class Sun extends GameObject {
    private final Color skyColor = new Color();
    private final Color sunColor = new Color();
    private final Color sunsetColor = new Color();

    private long lastTime;
    //todo позиция относительно мира а не игрока
    //public float x = 0, y = 0, worldX = (float) (Math.random() * (world.sizeX * TextureDrawing.blockSize));
    public float x = 0, y = 0, worldX = 0;

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

    //todo решить что делать с солнцем (!)
    public void update() {
        if (worldX >= (world.sizeX - Physics.swap) * TextureDrawing.blockSize) {
            worldX = 0;
        }
        //80
        if (lastTime == 0 || System.currentTimeMillis() - lastTime >= 10) {
            worldX++;
            lastTime = System.currentTimeMillis();
        }

        float parallax = world.sizeX / 1200f;
        int x = (int) ((worldX - DynamicObjects.getFirst().getX() * parallax + 1200) * parallax);

        double a = -0.0003;
        double b = 0.9;

        y = (float) (a * x * x + b * x) + 200;
        this.x = x - 930;

        final int minGreen = 85;
        final int maxGreen = 255;

        float ratio = (maxGreen - minGreen) / (2400f - minGreen);
        int green = (int) (maxGreen - (ratio));

        sunColor.set(255, green, 40, 220);
    }

    //todo
//    private void updateGradient() {
//        float alpha = 0;
//
//        if (currentTime >= startSunset && currentTime <= endSunset) {
//            alpha = lerp(0, 1, (currentTime - startSunset) / (endSunset - startSunset));
//        } else if (currentTime > endSunset) {
//            alpha = lerp(1, 0, (currentTime - endSunset) / (endSunset - startSunset));
//        }
//        int aGradient = (int) (250 * alpha);
//        aGradient = Math.max(0, Math.min(250, aGradient));
//
//        sunsetColor.set(aGradient, 0, 20, aGradient);
//    }

//    private void updateNightBackground() {
//        float alpha = 0;
//
//        if (currentTime >= endDay && currentTime <= startDay) {
//            alpha = lerp(0, 1, (currentTime - endDay) / (startDay - endDay));
//        } else if (currentTime > startDay) {
//            alpha = lerp(1, 0, (currentTime - startDay) / (startDay - endDay));
//        }
//        int aGradient = (int) (255 * alpha);
//        int deleteGradient = Math.max(0, Math.min(150, aGradient));
//        int backGradient = Math.max(0, Math.min(255, aGradient));
//
//        Color color = Color.fromRgba8888(deleteGradient, deleteGradient, deleteGradient, 0);
//        ShadowMap.deleteAllColor(color);
//        ShadowMap.deleteAllColorDynamic(color);
//
//        skyColor.set(255, 255, 255, backGradient);
//    }

    @Override
    public void draw() {
//        batch.draw(skyBackgroundTex, skyColor);
//        batch.draw(sunsetTex, sunsetColor);

        batch.z(-2);
        batch.draw(sunTex, sunColor, x, y);
    }
}
