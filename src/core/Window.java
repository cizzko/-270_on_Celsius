package core;

import core.EventHandling.EventHandler;
import core.EventHandling.MouseScrollCallback;
import core.Logging.config;
import core.Logging.logger;
import core.World.MainMenu;
import core.World.WorldGenerator;
import core.World.creatures.CreaturesGenerate;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import java.time.LocalDateTime;
import static core.World.Textures.TextureDrawing.*;
import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;

public class Window {
    public final int width, height;
    long lastFrameTime = System.currentTimeMillis();
    public static int deltaTime;
    private final String title, version = "dev 0.0.1";
    public static boolean start = false, fullScreen = Boolean.parseBoolean(config.jetFromConfig("FullScreen"));
    public static long glfwWindow;
    private static Window window;

    public Window() {
        this.width = Integer.parseInt(config.jetFromConfig("ScreenWidth"));
        this.height = Integer.parseInt(config.jetFromConfig("ScreenHeight"));
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
        glfwGetCurrentContext();
        GLFWErrorCallback.createPrint(System.err).set();
        if (glfwWindow == NULL) {
            //если окна не существует - создаст
            if (fullScreen) {
                glfwWindow = glfwCreateWindow(this.width, this.height, this.title, glfwGetPrimaryMonitor(), NULL);
            }
            else {
                glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
            }
            glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        } else {
            //при попытке создания окна возникла ошибка - сообщит
            logger.log("error at create window");
        }
        glfwMakeContextCurrent(glfwWindow);
        //vsync
        glfwSwapInterval(1);
        //настройка отображения
        glfwShowWindow(glfwWindow);
        //подключает инструменты библиотеки
        GL.createCapabilities();

        glMatrixMode(GL_PROJECTION);
        glOrtho(0, width, 0, height, 1, -1);
        glMatrixMode(GL_MODELVIEW);

        MainMenu.Create();
        glfwSetScrollCallback(glfwWindow, new MouseScrollCallback());

        logger.log("--------" + "\ninit: true" + "\nglfw version: " + glfwGetVersionString() + "\ngame version: " + version + "\ntime: " + LocalDateTime.now() + "\n--------");
    }

    public void draw() {
        logger.log("drawing started");
        WorldGenerator.generateWorld(1000, 20);
        WorldGenerator.generateDynamicsObjects();

        while (!glfwWindowShouldClose(glfwWindow)) {
            long currentTime = System.currentTimeMillis();
            deltaTime = (int) (currentTime - lastFrameTime);
            lastFrameTime = currentTime;

            if (EventHandler.getKey(GLFW_KEY_F1) && !start) {
                start = true;
                new Thread(new Physics()).start();
                new Thread(new CreaturesGenerate()).start();
            }
            if (start) {
                glClear(GL_COLOR_BUFFER_BIT);
                updateStaticObj();
                updateDynamicObj();
            }
            //updateGUI();
            glfwSwapBuffers(glfwWindow);

            if (EventHandler.getKey(GLFW_KEY_LEFT_ALT)) {
                System.exit(0);
            }
            glfwPollEvents();
        }
    }
}