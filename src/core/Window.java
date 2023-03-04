package core;

import core.Buttons.Buttons;
import core.Buttons.ButtonsObjects;
import core.World.MainMenu;
import core.World.Textures.TextureDrawing;
import core.World.Textures.TextureLoader;
import core.World.WorldObjects;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import java.awt.*;
import java.util.Hashtable;


import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;

public class Window {
    private final int width;
    private final int height;
    private final String title;
    public static long glfwWindow;
    private static Window window;

    public Window() {
        //чекает размеры твоего экрана
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.width = dim.width;
        this.height = dim.height;
        this.title = "-270 on Celsius";
    }

    public static Window get() {
        //если окно не существует (null), то оно создается
        if (Window.window == null) {
            Window.window = new Window();
        }
        return Window.window;
    }

    public void run() {
        init();
        loop();
    }

    public void init() {
        //инициализирует библиотеку
        glfwInit();
        System.out.println("'glfw' has been initialized");
        GLFWErrorCallback.createPrint(System.err).set();
        if (glfwWindow == NULL) {
            //если окна не существует - создаст
            glfwGetCurrentContext();
            glfwWindow = glfwCreateWindow(this.width, this.height, this.title, glfwGetPrimaryMonitor(), NULL);
        } else {
            //при попытке создания окна возникла ошибка - сообщит
            System.err.println("error at create glfwWindow");
        }
        glfwMakeContextCurrent(glfwWindow);
        //vsync
        glfwSwapInterval(1);
        //настройка отображения
        glfwShowWindow(glfwWindow);
        //подключает инструменты библиотеки
        GL.createCapabilities();

        glMatrixMode(GL_PROJECTION);
        glOrtho(0, width, height, 0, 1.0, -1.0);
        glMatrixMode(GL_MODELVIEW);

        MainMenu.Create();
    }

    public void loop() {
        final double FPS = 75.0; // Ограничение по числу кадров в секунду
        final double sigleFrameTime = 1.0 / FPS; // Время на формирование одного кадра
        double lastTime = 0.0; // Время начала формирования последнего кадра
        double currentTime; // Текущее время
        boolean start = false;
        WorldObjects[][] objects;

        Physics thread = new Physics();
        thread.setDaemon(true);

        glfwSwapBuffers(glfwWindow);
        glClear(GL_COLOR_BUFFER_BIT);

        //пока окно не закрыто - каждый такт опрашивает glfw
        while (!glfwWindowShouldClose(glfwWindow)) {
            currentTime = glfwGetTime();
            objects = thread.getWorldObjects();
            glfwPollEvents();

            if (start) {
                for (int x = 0; x < objects.length - 1; x++) {
                    for (int y = 0; y < objects[x].length - 1; y++) {
                        TextureDrawing.draw(objects[x][y].path, objects[x][y].x, objects[x][y].y, TextureLoader.ByteBufferEncoder(objects[x][y].path), TextureLoader.BufferedImageEncoder(objects[x][y].path));
                    }
                }
            }
            //f1
            else if (glfwGetKey(glfwWindow, 290) == 1) {
                thread.start();
                start = true;
            }
            if (currentTime - lastTime >= sigleFrameTime) {
                lastTime = currentTime;
                glfwSwapBuffers(glfwWindow);
            }
        }
    }
}