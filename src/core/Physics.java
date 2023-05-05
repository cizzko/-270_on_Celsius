package core;

import core.EventHandling.EventHandler;
import core.World.Textures.StaticWorldObjects;
import java.time.Duration;
import java.time.LocalDateTime;
import static core.Window.glfwWindow;
import static core.World.WorldGenerator.DynamicObjects;
import static core.World.WorldGenerator.StaticObjects;
import static org.lwjgl.glfw.GLFW.*;

public class Physics extends Thread {

    public void run() {
        while (!glfwWindowShouldClose(glfwWindow)) {
            try { Thread.sleep(16); } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            StaticWorldObjects staticObject = StaticObjects[(int) (DynamicObjects[0].x / 16)][(int) (DynamicObjects[0].y / 16)];

            if (DynamicObjects[0].isPlayer && EventHandler.getKey(GLFW_KEY_SPACE)) jump();
            if (DynamicObjects[0].isPlayer && EventHandler.getKey(GLFW_KEY_D) || EventHandler.getKey(GLFW_KEY_A)) move();

            if (DynamicObjects[0].isPlayer && !DynamicObjects[0].isJumping && DynamicObjects[0].y != staticObject.y && !staticObject.solid) {
                DynamicObjects[0].y--;
            }
        }
    }

    public static void jump() {
        if (!DynamicObjects[0].isJumping && DynamicObjects[0].isPlayer) {
            DynamicObjects[0].isJumping = true;

            float y0 = DynamicObjects[0].y;
            float yMax = y0 + 16; // максимальная высота прыжка 16 пикселей
            double g = 900.81; // скорость падения
            double timeToMax = Math.sqrt((2 * (yMax - y0)) / g); // время, необходимое для достижения максимальной высоты
            double totalTime = 2 * timeToMax; // общее время прыжка
            LocalDateTime startTime = LocalDateTime.now();

            while (true) {
                // Вычисление текущего времени от начала прыжка
                LocalDateTime currentTime = LocalDateTime.now();
                double elapsedTime = Duration.between(startTime, currentTime).toMillis() / 1000.0;

                // Проверка, достиг ли игрок максимальной высоты или полностью завершил прыжок
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
        }
    }

    public static void move() {
        if (EventHandler.getKey(GLFW_KEY_D)) DynamicObjects[0].x++;
        if (EventHandler.getKey(GLFW_KEY_A) && DynamicObjects[0].x > 0) DynamicObjects[0].x--;
    }
}