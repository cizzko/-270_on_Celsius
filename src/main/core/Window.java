package core;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Logger;
import core.EventHandling.MouseScrollCallback;
import core.UI.GUI.Menu.Main;
import core.World.Textures.TextureLoader;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL;
import java.nio.file.Path;
import static core.EventHandling.Logging.Logger.log;
import static core.World.Textures.TextureDrawing.*;
import static core.World.Textures.TextureLoader.BufferedImageEncoder;
import static core.World.Textures.TextureLoader.readImage;
import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;

public class Window {
    public static final String defPath = Path.of("").toAbsolutePath().toString().replace('\\', '/');
    public static int width = 1920, height = 1080, verticalSync = Config.getFromConfig("VerticalSync").equals("true") ? 1 : 0, fps = 0;
    public static final String version = "alpha 0.0.23 (non stable)";
    public static boolean start = false;
    public static long glfwWindow;

    public static String assetsDir(String path) {
        return defPath + "/src/assets/" + path.replace('\\', '/');
    }

    public static String pathTo(String path) {
        return defPath + path.replace('\\', '/');
    }

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

        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwMakeContextCurrent(glfwWindow);
        glfwSetScrollCallback(glfwWindow, new MouseScrollCallback());

        var cursorImage = readImage(BufferedImageEncoder(assetsDir("World/Other/cursorDefault.png")));
        GLFWImage glfwImg = GLFWImage.create().set(cursorImage.width(), cursorImage.height(), cursorImage.data());
        glfwSetCursor(glfwWindow, glfwCreateCursor(glfwImg, 0, 0));

        //vsync
        glfwSwapInterval(verticalSync);
        //display settings
        glfwShowWindow(glfwWindow);
        //connects library tools
        GL.createCapabilities();

        glMatrixMode(GL_PROJECTION);
        glOrtho(0, width, 0, height, 1, -1);
        glMatrixMode(GL_MODELVIEW);

        TextureLoader.preLoadResources();
        Main.create();

        log("Init status: true\n");
    }

    public void draw() {
        log("Thread: Main thread started drawing");

        glClearColor(206f / 255f, 246f / 255f, 1.0f, 1.0f);
        new EventHandler().start();

        while (!glfwWindowShouldClose(glfwWindow)) {
            updateVideo();
            if (start) {
                updateStaticObj();
                updateDynamicObj();
            } else {
                drawTexture(assetsDir("World/Other/background.png"), 0, 0, 1, true);
            }
            updateGUI();

            glfwSwapBuffers(glfwWindow);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glfwPollEvents();

            fps++;
        }
    }
}
