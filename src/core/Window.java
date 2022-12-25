package core;

import core.World.*;
import core.World.Textures.TextureDrawing;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.opengl.GL;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Hashtable;

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
            glfwWindow = glfwCreateWindow(this.width, this.height, this.title, glfwGetPrimaryMonitor(), NULL);
        }
        else {
            //при попытке создания окна возникла ошибка - сообщит
            System.err.println("error at create glfwWindow");
        }
        glfwMakeContextCurrent(glfwWindow);
        //vsync
        glfwSwapInterval(0);
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
        Physics thread = new Physics();
        World world = new World();
        thread.setDaemon(true);
        world.setDaemon(true);
        thread.start();
        world.start();

        glfwSwapBuffers(glfwWindow);
        glClear(GL_COLOR_BUFFER_BIT);

        //пока окно не закрыто - каждый такт опрашивает glfw
        while (!glfwWindowShouldClose(glfwWindow)) {
            glfwPollEvents();

        }
    }
}