package core.World.Creatures;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Logger;
import static core.Window.*;
import static core.World.HitboxMap.*;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.*;

public class Physics extends Thread {
    public static final int physicsSpeed = 1;
    private static float dropSpeed = 0;
    //min and default 1, max 4

    public void run() {
        Logger.log("Thread: Physics started");

        while (!glfwWindowShouldClose(glfwWindow)) {
            try { Thread.sleep(physicsSpeed); } catch (InterruptedException e) { throw new RuntimeException(e); }
            updateDrop();
            updateMove();
            updateJump();
        }
    }

    public static void setPlayerPos(int x, int y) {
        DynamicObjects[0].x = x == 0 ? DynamicObjects[0].x : x;
        DynamicObjects[0].y = y == 0 ? DynamicObjects[0].y : y;
    }

    public static void updateJump() {
        if (!DynamicObjects[0].isJumping && !DynamicObjects[0].isDropping && EventHandler.getKey(GLFW_KEY_SPACE)) {
            DynamicObjects[0].jump(64, 900);
        }
    }

    public static void updateMove() {
        if (EventHandler.getKey(GLFW_KEY_D) || EventHandler.getKey(GLFW_KEY_A)) {
            if (EventHandler.getKey(GLFW_KEY_D) && DynamicObjects[0].x < SizeX * 16 - 24 && !checkIntersStaticR(DynamicObjects[0].x, DynamicObjects[0].y, 24, 24)) {
                DynamicObjects[0].x += 0.1f;
            }
            if (EventHandler.getKey(GLFW_KEY_A) && DynamicObjects[0].x > 0 && !checkIntersStaticL(DynamicObjects[0].x, DynamicObjects[0].y, 24)) {
                DynamicObjects[0].x -= 0.1f;
            }
        }
    }

    private static void updateDrop() {
        if (!checkIntersStaticD(DynamicObjects[0].x, DynamicObjects[0].y, 24, 24)) {
            DynamicObjects[0].isDropping = true;
            dropSpeed += 0.001f;
            DynamicObjects[0].y -= dropSpeed;
        } else {
            dropSpeed = 0;
            DynamicObjects[0].isDropping = false;
        }
    }
}