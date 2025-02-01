package core;

import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Logger;
import core.g2d.*;
import core.input.InputHandler;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;

import static core.Global.*;
import static core.assets.TextureLoader.decodeImage;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public final class Window extends Application {
    public static int defaultWidth = 1920, defaultHeight = 1080;
    public static boolean windowFocused = true;
    public static long glfwWindow;
    public static Font defaultFont;

    @Override
    protected void init() throws Throwable {
        // Хмм, надо бы где-то тут создавать сцену
        assets.load(Font.class, "arial.ttf");
        assets.load(Atlas.class, "sprites");

        Config.checkConfig();
        if (Integer.parseInt(Config.getFromConfig("Debug")) >= 2) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
        }

        Logger.logStart();

        glfwSetErrorCallback(Global.app.keep(new GLFWErrorCallback() {
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
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

        glfwWindow = glfwCreateWindow(defaultWidth, defaultHeight, "-270 on Celsius", glfwGetPrimaryMonitor(), MemoryUtil.NULL);
        if (glfwWindow == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create window");
        }

        glfwMakeContextCurrent(glfwWindow);

        BufferedImage result;
        try (var in = Files.newInputStream(assets.assetsDir().resolve("World/Other/cursorDefault.png"))) {
            result = ImageIO.read(in);
        }
        try (var cursorImage = decodeImage(result);
             var stack = MemoryStack.stackPush()) {

            GLFWImage glfwImg = GLFWImage.malloc(stack);
            glfwImg.set(cursorImage.width(), cursorImage.height(), cursorImage.data());
            glfwSetCursor(glfwWindow, glfwCreateCursor(glfwImg, 0, 0));
        }

        if (Config.getFromConfig("VerticalSync").equals("true")) {
            Logger.log("Running with Vertical Sync");
            glfwSwapInterval(1);
        } else {
            glfwSwapInterval(0);
            int targetFPS = Integer.parseInt(Config.getFromConfig("TargetFPS"));
            Logger.log("Running with " + targetFPS + " fps");
            setFramerate(targetFPS);
        }

        GL.createCapabilities();

        // Великий инструмент.
        // glEnable(GL_DEBUG_OUTPUT);
        // keep(GLUtil.setupDebugMessageCallback());

        uiScene = new UiScene(defaultWidth, defaultHeight);
        input = new InputHandler(defaultWidth, defaultHeight);
        input.init();
        input.addListener(uiScene);

        glfwSetWindowFocusCallback(glfwWindow, Global.app.keep(new GLFWWindowFocusCallback() {
            @Override
            public void invoke(long window, boolean focused) {
                windowFocused = focused;
            }
        }));
        glfwSetWindowCloseCallback(glfwWindow, Global.app.keep(new GLFWWindowCloseCallback() {
            @Override
            public void invoke(long window) {
                quit();
            }
        }));

        batch = new SortingBatch(4 * 1024 * 1024, 1024 * 8, 1024 * 8);

        addListener(new AutoSaveListener());

        glClearColor(206f / 255f, 246f / 255f, 1.0f, 1.0f);
        glfwShowWindow(glfwWindow);

        setGameScene(new MenuScene());
    }

    @Override
    protected void update() {
        // Игровой цикл таков:
        // 1) фиксация deltaTime
        // 2) Считывание ввода
        // 3) Выполнение запланированных задач
        // 4) Обновление интерфейса
        // 5) Обновление мира
        //    1) Обновление статических объектов
        //    2) Обновление динамических объектов
        // TODO Почему порядок именно такой? Если думать о мире, как об объекте
        //   с которым динамические сущности взаимодействуют, то разве не должен быть обратным порядок?
        // 6) Отрисовка мира в порядке отображения
        updateTime();

        input.update();
        gameScene.loop();
        batch.flush();

        glfwSwapBuffers(glfwWindow);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        nextFrame();
    }

    @Override
    protected void cleanup() {
        glfwTerminate();
        batch.close();
        assets.unloadAll();
    }
}
