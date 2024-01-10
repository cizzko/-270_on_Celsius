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
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static core.EventHandling.Logging.Logger.log;
import static core.World.Textures.TextureDrawing.*;
import static core.World.Textures.TextureLoader.BufferedImageEncoder;
import static core.World.Textures.TextureLoader.readImage;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwInitHint;
import static org.lwjgl.opengl.GL13.*;

public class Window {
    public static final String defPath = Path.of("").toAbsolutePath().toString().replace('\\', '/'), versionStamp = "0.0.5", version = "alpha " + versionStamp + " (non stable)";
    public static int width = 1920, height = 1080, verticalSync = Config.getFromConfig("VerticalSync").equals("true") ? 1 : 0, fps = 0;
    public static boolean start = false;
    public static long glfwWindow;

    private static final List<NativeResource> resources = new ArrayList<>();

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

        glfwSetErrorCallback(addResource(new GLFWErrorCallback() {
            @Override
            public void invoke(int error, long description) {
                Logger.logExit(error, "Error at glfw: '" + error + "', with description: '" + GLFWErrorCallback.getDescription(description) + "'", false);
                System.exit(error);
            }
        }));

        switch (System.getenv("XDG_SESSION_TYPE")) {
            case "wayland" -> {
                glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_WAYLAND);
                glfwInitHint(GLFW_WAYLAND_LIBDECOR, GLFW_WAYLAND_DISABLE_LIBDECOR);
            }
            case null, default -> {}
        }
        glfwInit();
        glfwDefaultWindowHints();

        glfwWindow = glfwCreateWindow(width, height, "-270 on Celsius", glfwGetPrimaryMonitor(), MemoryUtil.NULL);

        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwMakeContextCurrent(glfwWindow);
        glfwSetScrollCallback(glfwWindow, addResource(new MouseScrollCallback()));

        var cursorImage = readImage(BufferedImageEncoder(assetsDir("World/Other/cursorDefault.png")));
        GLFWImage glfwImg = GLFWImage.create().set(cursorImage.width(), cursorImage.height(), cursorImage.data());
        addResource(glfwImg);
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

        EventHandler.init();
        TextureLoader.preLoadResources();
        Main.create();

        Global.input = new InputHandler();
        Global.input.init();

        log("Init status: true\n");
    }

    public static <R extends NativeResource> R addResource(R resource) {
        resources.add(resource);
        return resource;
    }

    public void draw() {
        log("Thread: Main thread started drawing");

        glClearColor(206f / 255f, 246f / 255f, 1.0f, 1.0f);
        while (!glfwWindowShouldClose(glfwWindow)) {
            Global.input.update();
            EventHandler.update();

            updateVideo();
            if (start) {
                updateStaticObj();
                updateDynamicObj();
            } else {
                drawTexture(0, 0, true, assetsDir("World/Other/background.png"));
            }
            updateGUI();

            glfwSwapBuffers(glfwWindow);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            fps++;
        }
        glfwTerminate();
        for (NativeResource resource : resources) {
            resource.free();
        }
    }
}
