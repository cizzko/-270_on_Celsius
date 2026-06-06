package core.World.StaticWorldObjects;

import core.util.Sized;

import static core.Global.*;
import static core.WorldCoordinates.*;

public class TemperatureMap {
    private static float[] temperature;
    private static final float coreTemp = 4000;
    public static float currentWorldTemperature;

    // todo как то криво работает
    public static void create() {
        currentWorldTemperature = 10;
        int sizeY = world.sizeY;
        temperature = new float[sizeY];

        float[] temp = temperature;
        for (int i = 0; i < temp.length; i++) {
            temp[i] = Math.min(coreTemp, (coreTemp / (i + 1)) * (sizeY / 1000f));
        }
    }

    public static float getTemp(int cellX, int cellY) {
        // thermal conductivity
        int n = 100;
        return Math.clamp(temperature[cellY] + (currentWorldTemperature / (temperature.length / (cellY + 1f))) / (101 - n), -270, coreTemp);
    }

    public static float getAverageTempAroundDynamic(float xPos, float yPos, Sized size) {
        int count = 0;
        float totalTemp = 0;

        int minX = toBlock(xPos);
        int minY = toBlock(yPos);

        int maxX = toBlock(xPos + toWorld(size.width()));
        int maxY = toBlock(yPos + toWorld(size.height()));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (world.inBounds(x, y)) {
                    totalTemp += getTemp(x, y);
                    count++;
                }
            }
        }
        return totalTemp / count;
    }
}
