package core.World.Background.DynamicBackground.Weather;

import core.math.Point2f;

import java.util.Arrays;
import java.util.HashSet;

import static core.World.WorldGenerator.*;

//todo increase the number of parameters to be processed
public class Wind {
    private static float[] windPower;
    private static int[] windDirection;
    private static HashSet<Point2f> whirlWinds = new HashSet<>();
    private static long lastUpdate = System.currentTimeMillis();

    //on starting world
    public static void createWind() {
        windPower = new float[SizeX];
        windDirection = new int[SizeX];

        Arrays.fill(windPower, 2);

        int lastSwapDirection = 200;
        //0 left, 1 right
        for (int i = 1; i < SizeX; i++) {
            if (Math.random() * lastSwapDirection > Math.max(400, SizeX / 4f)) {
                windDirection[i] = Math.abs(windDirection[i - 1] - 1);
                createWhirlwind(i);
                lastSwapDirection = 0;
            } else {
                lastSwapDirection++;
            }
        }
    }

    private static void createWhirlwind(int x) {
        float power = 0;

        for (int i = Math.max(x - 30, 0); i < Math.min(x + 30, SizeX); i++) {
            power += windPower[i];
        }

        whirlWinds.add(new Point2f(x, power));
    }

    private static void updateWhirlWinds(Point2f whirlWind) {
        windPower[(int) Math.max(whirlWind.x - 31, 1)] = 2;
        windPower[(int) Math.min(whirlWind.x + 31, SizeX - 1)] = 2;

        for (int i = (int) Math.max(whirlWind.x - 30, 1); i <= whirlWind.x; i++) {
            windPower[i] = ((windPower[i - 1] * 1.13f));
            windDirection[i] = 1;
        }

        for (int i = (int) whirlWind.x; i <= Math.min(whirlWind.x + 30, SizeX - 2); i++) {
            windPower[i] = ((windPower[i - 1] / 1.13f));
            windDirection[i] = 0;
        }
    }

    public static void updateWind() {
        if (System.currentTimeMillis() - lastUpdate >= 1000) {
            for (Point2f whirl : whirlWinds) {
                int shift = (int) (Math.random() * 4 - 2);

                whirl.x += shift;
                updateWhirlWinds(whirl);
            }
            lastUpdate = System.currentTimeMillis();
        }
    }
}
