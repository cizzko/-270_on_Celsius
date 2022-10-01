import World.Textures.TextureLoader;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import java.awt.*;
import java.nio.ByteBuffer;

import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Window {
    public ByteBuffer buffer;
    private int width, height;
    private String title;
    private long glfwWindow;
    private static Window window = null;

    private Window() {

        //чекает размеры твоего экрана
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.width = dim.width;
        this.height = dim.height;
        this.title = "-270 on Celsius";
    }

    public static Window get() {
        //если объект окна null, то оно создается
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
        //параметры создания окна, видимость, изменяемость размера
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);
        //создание окна
        glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
        //если не получается создать объект окна - ткнет носом
        if(glfwWindow == NULL){
            System.out.println("failed to create window");
        }
        glfwMakeContextCurrent(glfwWindow);
        //vsync
        glfwSwapInterval(1);
        //настройка отображения
        glfwShowWindow(glfwWindow);
        //подключает инструменты библиотеки
        GL.createCapabilities();

        TextureLoader.loadTexture("D:\\-270 on Celsius\\src\\assets\\TestImageForDrawing.png");
    }
    public void loop() {
        //в gl двойная буфферизация, меняет буффер и чистит его
        glfwSwapBuffers(glfwWindow);
        glClear(GL_COLOR_BUFFER_BIT);
        //пока окно не закрыто будет каждый такт опрашивать glfw
        while (!glfwWindowShouldClose(glfwWindow)) {
            glfwPollEvents();
            if (glfwGetKey(glfwWindow, 32) == 1) {

            }
        }
    }
}
