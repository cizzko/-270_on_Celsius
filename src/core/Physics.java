package core;

import core.EventHandling.EventHandler;
import core.World.WorldGenerator;
import core.World.WorldObjects;
import java.time.Duration;
import java.time.LocalDateTime;
import static core.Window.glfwWindow;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class Physics extends Thread {

    public void run() {
        WorldObjects[] DynamicObjects = WorldGenerator.DynamicObjects;

        while (!glfwWindowShouldClose(glfwWindow)) {
            if (DynamicObjects[0].player && EventHandler.getKey(GLFW_KEY_SPACE)){
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
                        break;
                    } else if (elapsedTime >= timeToMax) {
                        DynamicObjects[0].y = (float) (y0 + (yMax - y0) - 0.5 * g * Math.pow(elapsedTime - timeToMax, 2));
                    } else {
                        DynamicObjects[0].y = (float) (y0 + 0.5 * g * Math.pow(elapsedTime, 2));
                    }
                }
            }
        }
    }
}