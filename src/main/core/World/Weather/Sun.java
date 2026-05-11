package core.World.Weather;

import core.GameObject;
import core.Load;
import core.World.Creatures.Physics;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator.Biomes;
import core.g2d.Texture;
import core.graphic.Layer;
import core.util.Color;

import static core.EventHandling.Logging.Config.getFromConfigBool;
import static core.Global.*;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.World.WorldGenerator.WorldGenerator.DynamicObjects;

public class Sun extends GameObject {
    private final Color skyColor = new Color();
    private final Color sunColor = new Color();
    private final Color sunsetColor = new Color();
    //todo привязка
    private static final float scaleX = 1.5f, scaleY = 1.5f;
    //сначала сделать чтоб работало, потом делать красивее..
    //todo есть идейка с дублированием и связыванием задников надо обдумать
    private static int lastX, nextX;
    private static Biomes lastBiome = null;

    private long lastTime;
    //public float x = 0, y = 0, worldX = (float) (Math.random() * (world.sizeX * TextureDrawing.blockSize));
    public float x = 0, y = 0, worldX = 0;

    @Load("World/Sky/skyBackground0.png")
    protected Texture skyBackgroundTex;
    @Load
    protected Texture sunsetTex;
    @Load("World/Sun/sun.png")
    protected Texture sunTex;

    protected String sunsetTexName() {
        String sunsetType = getFromConfigBool("InterpolateSunset") ? "" : "non";
        return "World/Sun/" + sunsetType + "InterpolatedSunset.png";
    }

    //todo решить что делать с солнцем (!)
    //todo обновление: все плохо я не знаю как это делать без z координаты
    public void update() {
        Biomes currentBiome = world.getBiomes((int) DynamicObjects.getFirst().getX() / blockSize);

        if (currentBiome != lastBiome) {
            lastBiome = currentBiome;
            //lastX = (int) DynamicObjects.getFirst().getX() / blockSize;
        }

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

        batch.z(Layer.BACKGROUND);
        batch.pushState(() -> {
            batch.scale(scaleX * 2, scaleY * 2);
            batch.draw(atlas.byPath(lastBiome.getBackdrop()),((lastX - (DynamicObjects.getFirst().getX() / blockSize)) * 2) - 1500, 0);
            batch.scale(scaleX, scaleY);
            batch.draw(atlas.byPath(lastBiome.getBackdrop()),((lastX - (DynamicObjects.getFirst().getX() / blockSize)) * 3) - 1500, 0);
        });
        batch.z(-2);
        batch.draw(sunTex, sunColor, x, y);
    }
}
