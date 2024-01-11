package core.World.StaticWorldObjects;

import core.Utils.Sized;
import core.World.Textures.TextureDrawing;
import core.World.Weather.Sun;
import core.World.WorldGenerator;

import java.awt.*;
import java.util.HashMap;
import static core.World.Weather.Sun.currentTime;

public class TemperatureMap {
    private static HashMap<Point, Float> individualTemperature = new HashMap<>();
    private static float[] temperature;
    private static final float coreTemp = 4000;
    public static float dayTemperatureDecrement = 0.04f, currentWorldTemperature;

    public static void create() {
        currentWorldTemperature = currentTime / 100;
        temperature = new float[WorldGenerator.SizeY];

        for (int i = 0; i < temperature.length; i++) {
            temperature[i] = Math.min(coreTemp, (coreTemp / (i + 1)) * (WorldGenerator.SizeY / 1000f));
        }
    }

    public static HashMap<String, Object> getTemperatures() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("WorldTemperatureDecrement", dayTemperatureDecrement);
        map.put("WorldCurrentTemperature", currentWorldTemperature);
        map.put("WorldBlocksTemperature", temperature);
        map.put("WorldBlocksIndividualTemps", individualTemperature);

        return map;
    }

    public static void setData(HashMap<String, Object> map) {
        dayTemperatureDecrement = (float) map.get("WorldTemperatureDecrement");
        currentWorldTemperature = (float) map.get("WorldCurrentTemperature");
        temperature = (float[]) map.get("WorldBlocksTemperature");
        individualTemperature = (HashMap<Point, Float>) map.get("WorldBlocksIndividualTemps");
    }

    public static float getTemp(int cellX, int cellY) {
        float temp = individualTemperature.getOrDefault(new Point(cellX, cellY), Float.MIN_VALUE);

        if (temp != Float.MIN_VALUE) {
            if (temp == temperature[cellY]) {
                individualTemperature.remove(new Point(cellX, cellY));
            }
            return temp;
        }
        //thermal conductivity
        int n = 100;
        return Math.clamp(temperature[cellY] + (currentWorldTemperature / (temperature.length / (cellY + 1f))) / (101 - n), -270, coreTemp);
    }

    public static void setTemp(int cellX, int cellY, float temp) {
        if (temperature[cellY] != temp) {
            individualTemperature.put(new Point(cellX, cellY), temp);
        }
    }

    public static void update() {
        if (currentTime >= Sun.startDay || (currentTime >= 0 && currentTime <= Sun.endSunset)) {
            currentWorldTemperature += dayTemperatureDecrement / 2.6f;
        } else {
            currentWorldTemperature -= dayTemperatureDecrement;
        }
    }

    public static float getAverageTempAroundDynamic(float xPos, float yPos, Sized size) {
        int count = 0;
        float totalTemp = 0;

        for (int x = 0; x < Math.ceil(size.width()) / TextureDrawing.blockSize; x++) {
            for (int y = 0; y < Math.ceil(size.height()) / TextureDrawing.blockSize; y++) {
                totalTemp += getTemp((int) (xPos / TextureDrawing.blockSize + x), (int) (yPos / TextureDrawing.blockSize + y));
                count++;
            }
        }
        return totalTemp / count;
    }
}
