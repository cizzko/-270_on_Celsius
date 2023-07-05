package core;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.EventHandling.MouseScrollCallback;
import core.UI.GUI.CreateElement;
import core.UI.GUI.Fonts;
import core.UI.GUI.Menu.Main;
import core.EventHandling.Logging.Logger;
import core.World.Textures.TextureLoader;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import java.awt.*;
import java.nio.file.Paths;
import static core.EventHandling.Logging.Config.getFromConfig;
import static core.EventHandling.Logging.Logger.log;
import static core.World.Textures.TextureDrawing.*;
import static core.World.Weather.Sun.updateSun;
import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;

public class Window {
    public static String defPath = Paths.get("").toAbsolutePath().toString();

    public static int width = 1920, height = 1080, verticalSync;
    public static final String title = "-270 on Celsius", version = "dev 0.0.0.7";
    public static boolean start = false;
    public static long glfwWindow;

    public void run() {
        init();
        draw();
    }

    public void init() {
        Logger.logStart();

        verticalSync = Config.getFromConfig("VerticalSync").equals("true") ? 1 : 0;

        //инициализирует библиотеку
        glfwInit();
        glfwGetCurrentContext();
        GLFWErrorCallback.createPrint(System.err).set();

        glfwWindow = glfwCreateWindow(width, height, title, glfwGetPrimaryMonitor(), NULL);

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

        TextureLoader.preLoadTextures();
        Main.create();

        log("Init status: true");
    }

    public void draw() {
        log("Thread: Main thread started drawing");

        glClearColor(206f / 255f, 246f / 255f, 1.0f, 1.0f);
        new Thread(new EventHandler()).start();

        long lastSecondTime = System.currentTimeMillis();
        int framesThisSecond = 0;

        while (!glfwWindowShouldClose(glfwWindow)) {

            updateVideo();
            if (start) {
                updateSun();
                updateStaticObj();
                updateDynamicObj();
            } else {
                drawTexture(defPath + "\\src\\assets\\World\\other\\background.png", 0, 0, 1, true);
            }
            updateGUI();

            if (getFromConfig("Debug").equals("true") && System.currentTimeMillis() - lastSecondTime >= 1000) {
                CreateElement.createText(5, 1055, "FPS", "FPS: " + framesThisSecond, new Color(0, 0, 0, 255), null);
                framesThisSecond = 0;
                lastSecondTime = System.currentTimeMillis();
            }

            glfwSwapBuffers(glfwWindow);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glfwPollEvents();

            framesThisSecond++;
        }
    }
}