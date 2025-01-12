package core;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Logger;
import core.Utils.Color;
import core.Utils.NativeResources;
import core.World.Textures.TextureDrawing;
import core.World.Weather.Sun;
import core.World.Creatures.Physics;
import core.assets.TextureLoader;
import core.g2d.*;
import core.graphic.Layer;
import core.math.Rectangle;
import core.math.Vector2f;
import core.ui.Styles;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;

import static core.EventHandling.EventHandler.debugLevel;
import static core.EventHandling.Logging.Logger.log;
import static core.Global.*;
import static core.Utils.NativeResources.addResource;
import static core.World.Creatures.Player.Player.drawPlayerGui;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.World.WorldGenerator.DynamicObjects;
import static core.assets.TextureLoader.BufferedImageEncoder;
import static core.assets.TextureLoader.readImage;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class Window extends Application {
    public static final String versionStamp = "0.0.6", version = "alpha " + versionStamp + " (non stable)";
    public static int defaultWidth = 1920, defaultHeight = 1080;
    public static boolean start = false, windowFocused = true;
    public static long glfwWindow;
    public static Font defaultFont;

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

        TextureLoader.ImageData cursorImage = readImage(BufferedImageEncoder(assets.assetsDir("World/Other/cursorDefault.png")));
        GLFWImage glfwImg = GLFWImage.create().set(cursorImage.width(), cursorImage.height(), cursorImage.data());
        addResource(glfwImg);
        glfwSetCursor(glfwWindow, glfwCreateCursor(glfwImg, 0, 0));

        if (Config.getFromConfig("VerticalSync").equals("true")) {
            Logger.log("Running with Vertical Sync");
            glfwSwapInterval(1);
        } else {
            glfwSwapInterval(0);
            int targetFPS = Integer.parseInt(Config.getFromConfig("TargetFPS"));
            Logger.log("Running with " + targetFPS + " fps");
            setFramerate(targetFPS);
        }
        glfwShowWindow(glfwWindow);
        GL.createCapabilities();

        // Великий инструмент.
        // glEnable(GL_DEBUG_OUTPUT);
        // GLUtil.setupDebugMessageCallback();

        scene = new Scene(defaultWidth, defaultHeight);
        input = new InputHandler(defaultWidth, defaultHeight);
        input.init();
        input.addListener(scene);

        try {
            TextureLoader.preLoadResources();
        } catch (IOException e) {
            Logger.printException("Error when pre-loading resources", e);
        }

        glfwSetWindowFocusCallback(glfwWindow, addResource(new GLFWWindowFocusCallback() {
            @Override
            public void invoke(long window, boolean focused) {
                windowFocused = focused;
            }
        }));

        try {
            atlas = Atlas.load(assets.assetsDir("sprites"));
        } catch (IOException e) {
            Logger.printException("Error when loading texture atlas", e);
        }

        EventHandler.init();

        camera = new Camera2();
        camera.setToOrthographic(defaultWidth, defaultHeight);
        batch = new SortingBatch(4 * 1024 * 1024, 1024 * 8, 1024 * 8);
        batch.matrix(camera.projection);

        UI.mainMenu().show();

        addListener(new AutoSaveListener());

        log("Init status: true\n");
    }

    public void draw() {
        log("Thread: Main thread started drawing");

        glClearColor(206f / 255f, 246f / 255f, 1.0f, 1.0f);

        while (!glfwWindowShouldClose(glfwWindow)) {
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
            for (ApplicationListener listener : listeners) {
                try {
                    listener.update();
                } catch (Exception e) {
                    Logger.printException("Failed to update ApplicationListener: " + listener, e);
                }
            }

            EventHandler.inputUpdate();

            scheduler.executeAll();
            scene.update();

            if (start) {
                Physics.updatePhysics();
                TextureDrawing.updateWorld();

                batch.z(Layer.BACKGROUND);
                Sun.draw();
                batch.z(Layer.STATIC_OBJECTS);
                TextureDrawing.drawStatic();
                batch.z(Layer.DYNAMIC_OBJECTS);
                TextureDrawing.drawDynamic();

                debugInfo();
            } else {
                batch.draw(assets.getTextureByPath(assets.assetsDir("World/Other/background.png")));
            }

            scene.draw();
            drawPlayerGui();
            batch.flush();

            glfwSwapBuffers(glfwWindow);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            nextFrame();
        }

        glfwTerminate();
        NativeResources.terminateResources();
        batch.close();
    }

    static final Rectangle rect = new Rectangle();
    static final Vector2f vec = new Vector2f();
    static final Color green = Color.fromRgba8888(0, 255, 0, 255);
    static final Color red = Color.fromRgba8888(255, 0, 0, 255);
    static final Color blue = Color.fromRgba8888(0, 0, 255, 255);
    static final Color white = Color.fromRgba8888(255, 255, 255, 255);
    static final Color black = Color.fromRgba8888(0, 0, 0, 255);

    private void debugInfo() {
        if (debugLevel < 2) {
            return;
        }

        var player = DynamicObjects.getFirst();
        var size = player.getTexture();

        player.getHitboxTo(rect);
        var center = rect.getCenterTo(vec);

        int cx = (int) Math.floor(center.x / blockSize);
        int cy = (int) Math.floor(center.y / blockSize);

        float width = size.width();
        float height = size.height();
        int w = (int) Math.ceil(width / blockSize);
        int h = (int) Math.ceil(height / blockSize);

        int minX = (int) Math.floor(player.getX() / blockSize);
        int minY = (int) Math.floor(player.getY() / blockSize);

        int maxX = (int) Math.floor((player.getX() + width) / blockSize);
        int maxY = (int) Math.floor((player.getY() + height) / blockSize);

        TextureDrawing.drawText(player.getX(), player.getY() + size.height() - 32,
                "Fixture: " + player.hasFixture() + ", Velocity: " + player.velocity, black);

        // Интегрированный прямоугольник, который используется как хитбокс
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                Fill.rectangleBorder(x * blockSize, y * blockSize, blockSize, blockSize, white);
            }
        }

        TextureDrawing.drawText(player.getX(), player.getY() + size.height(),
                "Size: " + w + "x" + h + " (" + size.width() + "x" + size.height() + ")", Styles.DIRTY_BRIGHT_BLACK);

        // Ближайший к центру игрока блок
        if (false) Fill.rectangleBorder(cx * blockSize, cy * blockSize, blockSize, blockSize, green);
        // Прямоугольник, который показывает занятое текстурой пространство
        Fill.rectangleBorder(player.getX(), player.getY(), size.width(), size.height(),  red);

        // Две пересекающиеся перпендикулярные прямые, точкой пересечения которых является центр текстуры
        Fill.line(player.getX() + size.width()/2f, player.getY(), player.getX() + size.width()/2f, player.getY() + size.height(), blue);
        Fill.line(player.getX(), player.getY() + size.height() / 2f, player.getX() + size.width(), player.getY() + size.height() / 2f, blue);
    }
}
