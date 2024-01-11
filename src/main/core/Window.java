package core;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Logger;
import core.assets.AssetsManager;
import core.g2d.Atlas;
import core.g2d.Font;
import core.graphic.Layer;
import core.UI.GUI.Menu.Main;
import core.World.Textures.TextureDrawing;
import core.assets.TextureLoader;
import core.g2d.*;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static core.EventHandling.Logging.Logger.log;
import static core.Global.*;
import static core.assets.TextureLoader.BufferedImageEncoder;
import static core.assets.TextureLoader.readImage;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class Window {
    public static final String versionStamp = "0.0.5", version = "alpha " + versionStamp + " (non stable)";
    public static int defaultWidth = 1920, defaultHeight = 1080, verticalSync = Config.getFromConfig("VerticalSync").equals("true") ? 1 : 0, fps = 0;
    public static boolean start = false;
    public static long glfwWindow;

    public static Font defaultFont;

    private static final List<NativeResource> resources = new ArrayList<>();

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
                if (error != GLFW_FORMAT_UNAVAILABLE) {
                    System.exit(error);
                }
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

        glfwWindow = glfwCreateWindow(defaultWidth, defaultHeight, "-270 on Celsius", glfwGetPrimaryMonitor(), MemoryUtil.NULL);

        glfwMakeContextCurrent(glfwWindow);

        var cursorImage = readImage(BufferedImageEncoder(assets.assetsDir("World/Other/cursorDefault.png")));
        GLFWImage glfwImg = GLFWImage.create().set(cursorImage.width(), cursorImage.height(), cursorImage.data());
        addResource(glfwImg);
        glfwSetCursor(glfwWindow, glfwCreateCursor(glfwImg, 0, 0));

        //vsync
        glfwSwapInterval(verticalSync);
        //display settings
        glfwShowWindow(glfwWindow);
        //connects library tools
        GL.createCapabilities();

        // Великий инструмент.
        // glEnable(GL_DEBUG_OUTPUT);
        // GLUtil.setupDebugMessageCallback();

        EventHandler.init();
        try {
            TextureLoader.preLoadResources();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        input = new InputHandler();
        input.init();

        try {
            atlas = Atlas.load(assets.assetsDir("/out/sprites"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        camera = new Camera2();
        camera.setToOrthographic(defaultWidth, defaultHeight);
        batch = new SortingBatch(4 * 1024 * 1024, 1024 * 8, 1024 * 8);
        batch.matrix(camera.projection);

        Main.create();

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
            input.update();
            EventHandler.update();

            TextureDrawing.updateVideo();
            batch.z(Layer.STATIC_OBJECTS);
            if (start) {
                TextureDrawing.updateStaticObj();
                batch.z(Layer.DYNAMIC_OBJECTS);
                TextureDrawing.updateDynamicObj();
            } else {
                batch.draw(assets.getTextureByPath(assets.assetsDir("World/Other/background.png")));
            }
            batch.z(Layer.GUI);
            camera.setToOrthographic(camera.width(), camera.height());
            batch.matrix(camera.projection);
            TextureDrawing.updateGUI();
            batch.flush();

            glfwSwapBuffers(glfwWindow);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            fps++;
        }

        glfwTerminate();
        for (NativeResource resource : resources) {
            resource.free();
        }

        batch.close();
    }
}
