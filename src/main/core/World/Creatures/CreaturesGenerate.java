package core.World.Creatures;

import core.EventHandling.Logging.Logger;
import static core.Window.*;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class CreaturesGenerate {
    private static int currentCreaturesCount = 0;
    private static final int maxCreaturesCount = 20, generateSpeed = 400;
    private static long lastGenerate = System.currentTimeMillis();

    public static void initGenerating() {
        new Thread(() -> {
            Logger.log("Thread: Creatures logic started");

            long lastUpdate = System.nanoTime();

            while (!glfwWindowShouldClose(glfwWindow)) {
                if (System.nanoTime() - lastUpdate >= 1.0 / generateSpeed * 1000000000) {
                    lastUpdate = System.nanoTime();

                    if (System.currentTimeMillis() - lastGenerate >= 10000 && currentCreaturesCount < maxCreaturesCount && Math.random() * 30 < 1) {
                        generate();
                        lastGenerate = System.currentTimeMillis();
                    }
                    updateLogic();
                }
            }
        }).start();
    }

    private static void updateLogic() {
        for (DynamicWorldObjects object : DynamicObjects) {
            if (object != null) {
                if (object.x - 960 > SizeX * 16 || object.y - 540 > SizeY * 16 || object.x + 960 < 0 || object.y + 540 < 0) {
                    DynamicObjects.remove(object);
                    continue;
                }
                ButterflyLogic.update(object);
                BirdLogic.update(object);
            }
        }
    }

    public static void generate() {
        switch ((int) (Math.random() * 2)) {
            case 0 -> generateBird();
            case 1 -> generateButterfly();
        }
    }

    private static void generateButterfly() {
        DynamicObjects.add(new DynamicWorldObjects(false, true, 0.00002f, 2, 0.1f, (float) (Math.random() * (SizeX * 16)), 15, assetsDir("World/Creatures/butterfly")));
        currentCreaturesCount++;
    }

    private static void generateBird() {
        DynamicObjects.add(new DynamicWorldObjects(false, true, 0.0001f, 2, 0.1f, 24, SizeY * 13, assetsDir("World/Creatures/bird")));
        currentCreaturesCount++;
    }
}
