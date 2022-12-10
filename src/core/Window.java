package core;

import core.World.Textures.TextureDrawing;
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

    public void run(){

        init();
        loop();
    }

    public void init(){
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
        glClear(GL_COLOR_BUFFER_BIT);
        //пока окно не закрыто - каждый такт опрашивает glfw
        while (!glfwWindowShouldClose(glfwWindow)) {
            glfwWaitEvents();
            try {
                Thread.sleep(1000 / targerFps);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //считывание нажатой клавиши
            //f1 f2
            if (glfwGetKey(glfwWindow, 290) == 1) {
                Physics thread = new Physics();
                thread.setDaemon(true);
                thread.start();
                TextureDrawing.draw("D:\\-270_on_Celsius\\src\\assets\\World\\air.png", 1, 1, null, null);
            }
            if (glfwGetKey(glfwWindow, 291) == 1) {
                TextureDrawing.draw("D:\\-270_on_Celsius\\src\\assets\\World\\grass1.png", 1, 50, null, null);
            }
            glfwSwapBuffers(glfwWindow);
        }
    }
}