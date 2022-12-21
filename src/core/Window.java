package core;

import core.World.EventHandler;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import core.World.WorldObjects;
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
            glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
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
        glOrtho(0, 1000, 1000, 0, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);
    }
    public void loop() {
        Hashtable<String, ByteBuffer> byteBuffer = WorldGenerator.GenerateByteBuffer();
        Hashtable<String, BufferedImage> bufferedImage = WorldGenerator.GenerateBufferedImage();
        int x = 0;
        int y = 0;
        boolean start = false;
        WorldObjects[][] objects;
        Physics thread = new Physics();
        thread.setDaemon(true);

        glfwSwapBuffers(glfwWindow);
        glClear(GL_COLOR_BUFFER_BIT);

        //пока окно не закрыто - каждый такт опрашивает glfw
        while (!glfwWindowShouldClose(glfwWindow)) {
            glfwPollEvents();
            if (start == true) {
                objects = thread.getWorldObjects();
                TextureDrawing.draw(objects[x][y].path, objects[x][y].x, objects[x][y].y, byteBuffer.get(objects[x][y].path), bufferedImage.get(objects[x][y].path));
                x++;

                if (x == 50) {
                    y++;
                    x = 0;
                }
                if (y == 50) {
                    glfwSwapBuffers(glfwWindow);
                    y = 0;
                    x = 0;
                }
            }
            //f1
            else if (glfwGetKey(glfwWindow, 290) == 1) {
                thread.start();
                start = true;
            }
        }
    }
}