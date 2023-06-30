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
            if (System.currentTimeMillis() - deltaTime >= 10000 && count < 3 && Math.random() * 10000 < 0.01) {
                generate();
                deltaTime = System.currentTimeMillis();
            }
            ButteflyLogic.update();
            BirdLogic.update();
        }
    }

    public static void generate() {
        String path = "";
        //кто сделал этот костыль?
        //я
        //больше так не делай
        int rand = (int) (Math.random() * 2);
        if (rand == 1) path = defPath + "\\src\\assets\\World\\creatures\\bird";
        if (rand == 0) path = defPath + "\\src\\assets\\World\\creatures\\butterfly";

        for (int x = 0; x < DynamicObjects.length; x++) {
            if (DynamicObjects[x] == null) {
                DynamicObjects[x] = new DynamicWorldObjects(2, path.contains("bird") || path.contains("butterfly"), .1f, path, 60, SizeY / 2f + 520);
                count++;
                break;
            }
            if (DynamicObjects[x].x > SizeX * 16 || DynamicObjects[x].y > SizeY * 16) {
                DynamicObjects[x] = null;
            }
        }
    }
}
