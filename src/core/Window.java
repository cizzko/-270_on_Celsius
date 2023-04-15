package core;

import core.EventHandling.EventHandler;
import core.EventHandling.MouseScrollCallback;
import core.World.MainMenu;
import core.World.Sound;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import core.World.WorldObjects;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.awt.*;

import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;

public class Window {
    public final int width, height;
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
        draw();
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
        System.out.println(glfwGetVersionString());

        glMatrixMode(GL_PROJECTION);
        glOrtho(0, width, 0, height, 1, -1);
        glMatrixMode(GL_MODELVIEW);

        MainMenu.Create();
        glfwSetScrollCallback(glfwWindow, new MouseScrollCallback());
    }

    public void draw() {
        float cameraX = 1f;
        float cameraY = 1f;
        float zoom = 4f;
        boolean start = false;

        WorldGenerator.generateStaticObjects(1000, 20);
        WorldGenerator.generateDynamicsObjects();
        WorldObjects[][] objects = WorldGenerator.StaticObjects;

        glfwSwapBuffers(glfwWindow);
        glClear(GL_COLOR_BUFFER_BIT);

        //пока окно не закрыто
        while (!glfwWindowShouldClose(glfwWindow)) {
            //alt
            if (EventHandler.getKey(GLFW_KEY_LEFT_ALT)){
                System.exit(0);
            }

            //Это настолько костыльно, что надо переписать
            if (start) {
                glClear(GL_COLOR_BUFFER_BIT);
                for (int x = 0; x < objects.length - 1; x++) {
                    for (int y = 0; y < objects[x].length - 1; y++) {

                        float left = cameraX - width / zoom;
                        float right = cameraX + width / zoom;
                        float top = cameraY - height / zoom;
                        float bottom = cameraY + height / zoom;

                        objects[x][y].onCamera = !(objects[x][y].x < left) && !(objects[x][y].x > right) && !(objects[x][y].y < top) && !(objects[x][y].y > bottom);

                        if (EventHandler.getKey(GLFW_KEY_1)) zoom += 0.000005f;
                        if (EventHandler.getKey(GLFW_KEY_2)) zoom -= 0.000005f;

                        if (objects[x][y].onCamera) {
                            TextureDrawing.draw(objects[x][y].path, (int) objects[x][y].x, (int) objects[x][y].y, zoom);
                        }
                    }
                }
                for (int i = 0; i < WorldGenerator.DynamicObjects.length; i++) {
                    if (WorldGenerator.DynamicObjects[i] != null && WorldGenerator.DynamicObjects[i].onCamera){
                        TextureDrawing.draw(WorldGenerator.DynamicObjects[i].path, (int) WorldGenerator.DynamicObjects[i].x, (int) WorldGenerator.DynamicObjects[i].y, zoom);
                    }
                }
                glfwSwapBuffers(glfwWindow);
            }
            //f1
            else if (EventHandler.getKey(GLFW_KEY_F1)) {
                new Thread(new Physics()).start();
                start = true;
            }
            glfwPollEvents();
        }
    }
}