package core.World.Creatures;

import core.EventHandling.Logging.Logger;
import core.World.Textures.DynamicWorldObjects;
import static core.Window.defPath;
import static core.Window.glfwWindow;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class CreaturesGenerate extends Thread {
    private static int count = 0;
    private static long deltaTime = System.currentTimeMillis();

    public void run() {
        Logger.log("Thread: Creatures logic started");

        while (!glfwWindowShouldClose(glfwWindow)) {
            for (int i = 1; i < DynamicObjects.length; i++) {
                if (DynamicObjects[i] != null && (DynamicObjects[i].x - 960 > SizeX * 16 || DynamicObjects[i].y - 540 > SizeY * 16 || DynamicObjects[i].x + 960 < 0 || DynamicObjects[i].y + 540 < 0)) {
                    DynamicObjects[i] = null;
                }
            }

            if (System.currentTimeMillis() - deltaTime >= 10000 && count < 4 && Math.random() * 30 < 1) {
                generate();
                deltaTime = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - deltaTime >= 10000) {
                deltaTime = System.currentTimeMillis();
            }
            ButteflyLogic.update();
            BirdLogic.update();
        }
    }

    public static void generate() {
        switch ((int) (Math.random() * 2)) {
            case 0 -> generateBird();
            case 1 -> generateButterfly();
        }
    }

    public static void generate(int s) {
        generateBird();
    }

    public static void generateButterfly() {
        for (int x = 0; x < DynamicObjects.length; x++) {
            if (DynamicObjects[x] == null) {
                DynamicObjects[x] = new DynamicWorldObjects(true, 2, 0.1f, (float) (Math.random() * (SizeX * 16)), 15, x, defPath + "\\src\\assets\\World\\creatures\\butterfly");
                count++;
                break;
            }
        }
    }

    public static void generateBird() {
        for (int x = 0; x < DynamicObjects.length; x++) {
            if (DynamicObjects[x] == null) {
                DynamicObjects[x] = new DynamicWorldObjects(true, 2, 0.1f, 24, SizeY * 13, 40, x,  defPath + "\\src\\assets\\World\\creatures\\bird");
                count++;
                break;
            }
        }
    }
}
