package core.World.Creatures;

import core.EventHandling.Logging.Logger;

import static core.Window.*;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class CreaturesGenerate {
    private static int currentCreaturesCount = 0;
    private static final int maxCreaturesCount = 20;
    private static long deltaTime = System.currentTimeMillis();

    public static void initGenerating() {
        new Thread(() -> {
            Logger.log("Thread: Creatures logic started");

            while (!glfwWindowShouldClose(glfwWindow)) {
                for (int i = 1; i < DynamicObjects.size(); i++) {
                    if (DynamicObjects.get(i) != null && (DynamicObjects.get(i).x - 960 > SizeX * 16 || DynamicObjects.get(i).y - 540 > SizeY * 16 || DynamicObjects.get(i).x + 960 < 0 || DynamicObjects.get(i).y + 540 < 0)) {
                        DynamicObjects.remove(i);
                    }
                }

                if (System.currentTimeMillis() - deltaTime >= 10000 && currentCreaturesCount < maxCreaturesCount && Math.random() * 30 < 1) {
                    generate();
                    deltaTime = System.currentTimeMillis();
                }
                ButteflyLogic.update();
                BirdLogic.update();
            }
        }).start();
    }

    public static void generate() {
        switch ((int) (Math.random() * 2)) {
            case 0 -> generateBird();
            case 1 -> generateButterfly();
        }
    }

    public static void generateButterfly() {
        DynamicObjects.add(new DynamicWorldObjects(false, true, 0.00002f, 2, 0.1f, (float) (Math.random() * (SizeX * 16)), 15, assetsDir("World/Creatures/butterfly")));
        currentCreaturesCount++;
    }

    public static void generateBird() {
        DynamicObjects.add(new DynamicWorldObjects(false, true, 0.0001f, 2, 0.1f, 24, SizeY * 13, assetsDir("World/Creatures/bird")));
        currentCreaturesCount++;
    }
}
