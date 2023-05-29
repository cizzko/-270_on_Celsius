package core;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.json;
import core.EventHandling.MouseScrollCallback;
import core.UI.GUI.Fonts;
import core.UI.GUI.Video;
import core.EventHandling.Logging.config;
import core.EventHandling.Logging.logger;
import core.UI.GUI.Menu.Main;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import java.nio.file.Paths;
import static core.World.Textures.TextureDrawing.*;
import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;

public class Window {
    public static String defPath = Paths.get("").toAbsolutePath().toString();

    public static int width = Integer.parseInt(config.jetFromConfig("ScreenWidth")), height = Integer.parseInt(config.jetFromConfig("ScreenHeight")), deltaTime, verticalSync;
    public static final String title = "-270 on Celsius", version = "dev 0.0.0.5";
    public static boolean start = false, fullScreen = Boolean.parseBoolean(config.jetFromConfig("FullScreen"));
    public static long glfwWindow, lastFrameTime = System.currentTimeMillis(), totalFrames;

    public void run() {
        init();
        draw();
    }

    public void init() {
        logger.logStart();
        json.getAllLanguages();

        if (config.jetFromConfig("VerticalSync").equals("true")) {
            verticalSync = 1;
        } else {
            verticalSync = 0;
        }

        //инициализирует библиотеку
        glfwInit();
        glfwGetCurrentContext();
        GLFWErrorCallback.createPrint(System.err).set();

        if (fullScreen) {
            glfwWindow = glfwCreateWindow(width, height, title, glfwGetPrimaryMonitor(), NULL);
        } else {
            glfwWindow = glfwCreateWindow(width, height, title, NULL, NULL);
        }

        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwMakeContextCurrent(glfwWindow);
        glfwSetScrollCallback(glfwWindow, new MouseScrollCallback());
        //vsync
        glfwSwapInterval(verticalSync);
        //настройка отображения
        glfwShowWindow(glfwWindow);
        //подключает инструменты библиотеки
        GL.createCapabilities();

        glMatrixMode(GL_PROJECTION);
        glOrtho(0, width, 0, height, 1, -1);
        glMatrixMode(GL_MODELVIEW);

        Fonts.generateFont(defPath + "\\src\\assets\\UI\\arial.ttf");
        Video.drawVideo(defPath + "\\src\\assets\\World\\kaif.mp4", 1, 30, 0, 0, 1920, 1080);

        Main.create();
        logger.log("Init status: true");
    }

    public void draw() {
        logger.log("Thread: Main thread started drawing");

        glClearColor(206f / 255f, 246f / 255f, 1.0f, 1.0f);
        new Thread(new EventHandler()).start();

        while (!glfwWindowShouldClose(glfwWindow)) {
            long currentTime = System.currentTimeMillis();
            deltaTime = (int) (currentTime - lastFrameTime);
            lastFrameTime = currentTime;

            updateVideo();
            if (start) {
                updateStaticObj();
                updateDynamicObj();
            }
            updateGUI();

            glfwSwapBuffers(glfwWindow);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glfwPollEvents();

            totalFrames++;
        }
    }
}