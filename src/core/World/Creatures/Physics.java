package core.World.Creatures;

import core.EventHandling.Logging.Logger;
import core.UI.GUI.Menu.Pause;
import core.UI.GUI.Menu.Settings;
import static core.Window.*;
import static core.World.Creatures.Player.*;
import static core.World.HitboxMap.*;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.*;

public class Physics extends Thread {
    public static int physicsSpeed = 400, updates = 0;
    private static boolean stop = false;
    public static int lastSpeed = physicsSpeed;
    //default 400

    public void run() {
        Logger.log("Thread: Physics started");

        long lastUpdateTime = System.nanoTime();

        while (!glfwWindowShouldClose(glfwWindow)) {
            if (System.nanoTime() - lastUpdateTime >= 1.0 / physicsSpeed * 1000000000) {
                updateCreaturesPhys();
                updatePlayerPhys();

                updates++;
                lastUpdateTime = System.nanoTime();
            }
            if ((Settings.createdSettings || Pause.created) && !stop) {
                lastSpeed = physicsSpeed;
                physicsSpeed = 1;
                stop = true;
            } else if (!Settings.createdSettings && !Pause.created && stop) {
                physicsSpeed = lastSpeed;
                stop = false;
            }
        }
    }

    private static void updateDrop() {
        for (core.World.Textures.DynamicWorldObjects dynamicObject : DynamicObjects) {
            if (dynamicObject == null || dynamicObject.isFlying || dynamicObject.path.contains("player")) {
                continue;
            }
            if (!checkIntersStaticD(dynamicObject.x, dynamicObject.y, 24, 24)) {
                dynamicObject.isDropping = true;
                dynamicObject.dropSpeed += 0.001f;
                dynamicObject.y -= dynamicObject.dropSpeed;
            } else {
                dynamicObject.dropSpeed = 0;
                dynamicObject.isDropping = false;
            }
        }
    }

    private static void updatePlayerPhys() {
        updatePlayerMove();
        updatePlayerJump();
        updatePlayerDrop();
        //updateDestroyBlocks();
    }

    private static void updateCreaturesPhys() {
        updateDrop();
    }
}