package core.World.Creatures;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Logger;
import core.World.Textures.StaticWorldObjects;
import java.time.Duration;
import java.time.LocalDateTime;
import static core.Window.glfwWindow;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.*;

public class Physics extends Thread {
    private static boolean isDropping = false;
    private static long currentTime = System.currentTimeMillis();

    public void run() {
        Logger.log("Thread: Physics started");

        while (!glfwWindowShouldClose(glfwWindow)) {
            try { Thread.sleep(16); } catch (InterruptedException e) { throw new RuntimeException(e); }
            updateDrop();
            updateMove();
            updateJump();
        }
    }

    public static void updateJump() {
        if (!DynamicObjects[0].isJumping && DynamicObjects[0].isPlayer && !isDropping && EventHandler.getKey(GLFW_KEY_SPACE)) {
            DynamicObjects[0].isJumping = true;
            new Thread(() -> {

                float y0 = DynamicObjects[0].y;
                float yMax = y0 + 160; // максимальная высота прыжка 16 пикселей
                double g = 900.81; // скорость падения
                double timeToMax = Math.sqrt((2 * (yMax - y0)) / g); // время, необходимое для достижения максимальной высоты
                double totalTime = 2 * timeToMax; // общее время прыжка
                LocalDateTime startTime = LocalDateTime.now();

                while (true) {
                    StaticWorldObjects staticObject = StaticObjects[(int) (DynamicObjects[0].x / 16)][(int) (DynamicObjects[0].y / 16)];

                    if (DynamicObjects[0].isPlayer && !DynamicObjects[0].isJumping && staticObject.solid) {
                        break;
                    }

                    LocalDateTime currentTime = LocalDateTime.now();
                    double elapsedTime = Duration.between(startTime, currentTime).toMillis() / 1000.0;

                    if (elapsedTime >= totalTime) {
                        DynamicObjects[0].y = y0;
                        DynamicObjects[0].isJumping = false;
                        break;
                    } else if (elapsedTime >= timeToMax) {
                        DynamicObjects[0].y = (float) (y0 + (yMax - y0) - 0.5 * g * Math.pow(elapsedTime - timeToMax, 2));
                    } else {
                        DynamicObjects[0].y = (float) (y0 + 0.5 * g * Math.pow(elapsedTime, 2));
                    }
                }
            }).start();
        }
    }

    public static void updateMove() {
        if (System.currentTimeMillis() >= currentTime + 16 && DynamicObjects[0].isPlayer && EventHandler.getKey(GLFW_KEY_D) || EventHandler.getKey(GLFW_KEY_A)) {
            if (EventHandler.getKey(GLFW_KEY_D) && DynamicObjects[0].x < SizeX * 16 - 24) DynamicObjects[0].x++;
            if (EventHandler.getKey(GLFW_KEY_A) && DynamicObjects[0].x > 0) DynamicObjects[0].x--;

            currentTime = System.currentTimeMillis();
        }
    }

    private static void updateDrop() {
        StaticWorldObjects staticObject = StaticObjects[(int) (DynamicObjects[0].x / 16)][(int) (DynamicObjects[0].y / 16)];

        if (DynamicObjects[0].isPlayer && !DynamicObjects[0].isJumping && !staticObject.solid) {
            isDropping = true;
            DynamicObjects[0].y--;
        } else {
            isDropping = false;
        }
    }
}