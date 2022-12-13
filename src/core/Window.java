package core;

import core.World.Textures.TextureDrawing;
import core.World.WorldObjects;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import java.awt.*;

import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;

public class Window {
    private int width, height;
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
        if(glfwWindow == NULL){
            //если окна не существует - создаст
            glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
        }
        else {
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

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        glMatrixMode(GL_PROJECTION);
        glOrtho(0, dim.height, dim.width, 0, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);
    }
    public void loop() {
        int targerFps = 75;
        int x = 0;
        int y = 0;
        boolean start = false;
        WorldObjects[][] objects;
        Physics thread = new Physics();
        thread.setDaemon(true);

        glClear(GL_COLOR_BUFFER_BIT);
        //пока окно не закрыто - каждый такт опрашивает glfw
        while (!glfwWindowShouldClose(glfwWindow)) {
            glfwPollEvents();
            try {
                Thread.sleep(1000 / targerFps);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //после нажатия ф1 стартует
            if(glfwGetKey(glfwWindow, 290) == 1) {
                thread.start();
                start = true;
            }
            if(start == true) {
                objects = thread.getWorldObjects();
                TextureDrawing.draw(objects[x][y].path, objects[x][y].x, objects[x][y].y, null, null);
                x++;

                if(x == 30){
                    y++;
                    x = 0;
                }
                if(y == 30){
                    y = 0;
                    x = 0;
                }
            }
            glfwSwapBuffers(glfwWindow);
        }
    }
}