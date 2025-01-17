package core.World.Creatures;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Logger;
import core.World.Textures.TextureDrawing;

import static core.Global.world;
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
                if (object.getX() - 960 > world.sizeX * TextureDrawing.blockSize || object.getY() - 540 > world.sizeY * TextureDrawing.blockSize || object.getX() + 960 < 0 || object.getY() + 540 < 0) {
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
            // todo переписать поведение птиц
            // case 0 -> generateBird();
            case 1 -> generateButterfly();
        }
        currentCreaturesCount++;
    }

    private static void generateButterfly() {
        DynamicObjects.add(DynamicWorldObjects.createDynamic("butterfly", (float) (Math.random() * (world.sizeX * TextureDrawing.blockSize))));
    }

    private static void generateBird() {
        DynamicObjects.add(DynamicWorldObjects.createDynamic("bird", 100));
    }
}
