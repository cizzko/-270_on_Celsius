package core;

import core.util.Color;
import core.World.Creatures.DynamicWorldObjects;
import core.World.StaticWorldObjects.TemperatureMap;
import core.g2d.Texture;

import static core.Global.batch;
import static core.World.WorldGenerator.DynamicObjects;

public final class PostEffect extends GameObject {
    private final Color temperatureColor = new Color();

    @Load("UI/GUI/modifiedTemperature.png")
    private Texture temperatureTex;

    @Override
    public void update() {
        DynamicWorldObjects player = DynamicObjects.getFirst();
        int temp = (int) TemperatureMap.getAverageTempAroundDynamic(player.getX(), player.getY(), player.getTexture());
        int upperLimit = 100;
        int lowestLimit = -20;
        int maxColor = 90;

        int a;
        if (temp > upperLimit) {
            a = Math.min(maxColor, Math.abs((temp - upperLimit) / 3));
        } else if (temp < lowestLimit) {
            a = Math.min(maxColor, Math.abs((temp + lowestLimit) / 3));
        } else {
            a = 0;
        }

        int r = temp > 0 ? a : 0;
        int b = temp > 0 ? 0 : a;
        temperatureColor.set(r, (int) (b / 2f), b, a);
    }

    @Override
    public void draw() {
        batch.draw(temperatureTex, temperatureColor);
    }
}
