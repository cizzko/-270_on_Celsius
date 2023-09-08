package core;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.EventHandling.MouseScrollCallback;
import core.UI.GUI.Fonts;
import core.UI.GUI.Menu.Main;
import core.EventHandling.Logging.Logger;
import core.World.Textures.TextureLoader;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import java.nio.file.Paths;
import static core.EventHandling.Logging.Logger.log;
import static core.World.Textures.TextureDrawing.*;
import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;

public class Window {
    public final static String defPath = Paths.get("").toAbsolutePath().toString();
    public static int width = 1920, height = 1080, verticalSync = Config.getFromConfig("VerticalSync").equals("true") ? 1 : 0, fps = 0;
    public static final String version = "dev 0.0.0.9";
    public static boolean start = false;
    public static long glfwWindow;

    public void run() {
        init();
        draw();
    }

    public void init() {
        Logger.logStart();

        glfwSetErrorCallback(new GLFWErrorCallback() {
            @Override
            public void invoke(int error, long description) {
                Logger.logExit(error, "Error at glfw: '" + error + "', with description: '" + GLFWErrorCallback.getDescription(description) + "'", false);
                System.exit(error);
            }
        });

        glfwInit();
        glfwWindow = glfwCreateWindow(width, height, "-270 on Celsius", glfwGetPrimaryMonitor(), NULL);
        glfwSetInputMode(glfwWindow, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwMakeContextCurrent(glfwWindow);
        glfwSetScrollCallback(glfwWindow, new MouseScrollCallback());
        //glfwSetScrollCallback(glfwWindow, new MouseScrollCallback());
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
        TextureLoader.bindChars();
        Main.create();

        log("Init status: true");
    }

    public void draw() {
        log("Thread: Main thread started drawing");

        glClearColor(206f / 255f, 246f / 255f, 1.0f, 1.0f);
        new Thread(new EventHandler()).start();

        while (!glfwWindowShouldClose(glfwWindow)) {
            updateVideo();
            if (start) {
                updateStaticObj();
                updateDynamicObj();
            } else {
                drawTexture(defPath + "\\src\\assets\\World\\other\\background.png", 0, 0, 1, true);
            }
            updateGUI();
            drawCursor();

            glfwSwapBuffers(glfwWindow);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glfwPollEvents();

            fps++;
        }
    }
}