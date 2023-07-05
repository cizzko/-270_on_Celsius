package core.World.Creatures;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Logger;
import core.UI.GUI.CreateElement;
import core.UI.GUI.Menu.Pause;
import core.UI.GUI.Menu.Settings;

import java.awt.*;
import static core.Window.*;
import static core.World.HitboxMap.*;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.*;

public class Physics extends Thread {
    public static int physicsSpeed = 400;
    private static boolean stop = false;
    private static int lastSpeed = 400;
    //default 400

    public void run() {
        Logger.log("Thread: Physics started");

        long updates = 0;
        long lastSecond = System.currentTimeMillis();
        long lastUpdateTime = System.nanoTime();

        while (!glfwWindowShouldClose(glfwWindow)) {
            double targetFps = 1.0 / physicsSpeed * 1000000000;

            if (System.nanoTime() - lastUpdateTime >= targetFps) {
                if (Config.getFromConfig("Debug").equals("true") && System.currentTimeMillis() - lastSecond >= 1000) {
                    CreateElement.createText(5, 1020, "PhysicsUpdate", "Physics FPS: " + updates, new Color(0, 0, 0, 255), null);
                    lastSecond = System.currentTimeMillis();
                    updates = 0;
                }

                updateDrop();
                updateMove();
                updateJump();

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

    public static void setPlayerPos(int x, int y) {
        DynamicObjects[0].x = x == 0 ? DynamicObjects[0].x : x;
        DynamicObjects[0].y = y == 0 ? DynamicObjects[0].y : y;
    }

    public static void updateJump() {
        if (EventHandler.getKey(GLFW_KEY_SPACE)) {
            DynamicObjects[0].jump(52, 600);
        }
    }

    public static void updateMove() {
        if (EventHandler.getKey(GLFW_KEY_D) || EventHandler.getKey(GLFW_KEY_A)) {
            if (EventHandler.getKey(GLFW_KEY_D) && DynamicObjects[0].x < SizeX * 16 - 24 && !checkIntersStaticR(DynamicObjects[0].x + 0.1f, DynamicObjects[0].y, 24, 24)) {
                DynamicObjects[0].x += 0.1f;
            }
            if (EventHandler.getKey(GLFW_KEY_A) && DynamicObjects[0].x > 0 && !checkIntersStaticL(DynamicObjects[0].x - 0.1f, DynamicObjects[0].y, 24)) {
                DynamicObjects[0].x -= 0.1f;
            }
        }
    }

    private static void updateDrop() {
        for (core.World.Textures.DynamicWorldObjects dynamicObject : DynamicObjects) {
            if (dynamicObject == null || dynamicObject.isFlying) {
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
}