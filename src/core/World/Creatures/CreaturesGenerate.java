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

    public static void generateButterfly() {
        for (int x = 0; x < DynamicObjects.length; x++) {
            if (DynamicObjects[x] == null) {
                DynamicObjects[x] = new DynamicWorldObjects(2, true, defPath + "\\src\\assets\\World\\creatures\\butterfly", 0.1f, (float) (Math.random() * (SizeX * 16)));
                count++;
                break;
            }
            if (DynamicObjects[x].x > SizeX * 16 || DynamicObjects[x].y > SizeY * 16) {
                DynamicObjects[x] = null;
            }
        }
    }

    public static void generateBird() {
        for (int x = 0; x < DynamicObjects.length; x++) {
            if (DynamicObjects[x] == null) {
                DynamicObjects[x] = new DynamicWorldObjects(2, true, defPath + "\\src\\assets\\World\\creatures\\bird", 0.1f, 24);
                count++;
                break;
            }
            if (DynamicObjects[x].x > SizeX * 16 || DynamicObjects[x].y > SizeY * 16) {
                DynamicObjects[x] = null;
            }
        }
    }
}
