package core;

import com.sun.management.OperatingSystemMXBean;
import core.EventHandling.Logging.Config;
import core.World.ContentManager;
import core.g2d.*;
import core.input.InputHandler;
import core.util.DebugTools;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.io.IoBuilder;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;

import static core.Global.*;
import static core.assets.TextureLoader.decodeImage;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public final class Window extends Application {
    private static final Logger lwjglLogger = LogManager.getLogger("org.lwjgl.LWJGL");

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
            Configuration.DEBUG_STREAM.set(IoBuilder.forLogger(lwjglLogger)
                    .setLevel(Level.DEBUG)
                    .buildPrintStream());
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
        }

        glfwSetErrorCallback(Global.app.keep(new GLFWErrorCallback() {
            private final Marker GLFW = MarkerManager.getMarker("GLFW");
            private final Int2ObjectOpenHashMap<String> ERROR_CODES;
            {
                ERROR_CODES = new Int2ObjectOpenHashMap<>(APIUtil.apiClassTokens((field, value) -> 0x10000 < value && value < 0x20000, null, org.lwjgl.glfw.GLFW.class));
                ERROR_CODES.trim();
            }

            @Override
            public void invoke(int error, long description) {
                String errorStr = ERROR_CODES.get(error);
                String msg = getDescription(description);
                lwjglLogger.error(GLFW, "error code: {}, description: {}", errorStr, msg);

                StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                for (int i = 4; i < stack.length; i++) {
                    lwjglLogger.error(GLFW,"\tat {}", stack[i]);
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

        printComputerInfo();

        if (Config.getFromConfig("VerticalSync").equals("true")) {
            log.info("Framerate: Vertical Sync");
            glfwSwapInterval(1);
        } else {
            glfwSwapInterval(0);
            int targetFPS = Integer.parseInt(Config.getFromConfig("TargetFPS"));
            log.info("Framerate: {} fps", targetFPS);
            setFramerate(targetFPS);
        }

        GL.createCapabilities();

        // Великий инструмент.
        // if (Integer.parseInt(Config.getFromConfig("Debug")) >= 2) {
        //     glEnable(GL_DEBUG_OUTPUT);
        //     keep(GLUtil.setupDebugMessageCallback());
        // }

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
        content = new ContentManager();
        content.loadAll(); // TODO

        addListener(new AutoSaveListener());

        glClearColor(206f / 255f, 246f / 255f, 1.0f, 1.0f);
        glfwShowWindow(glfwWindow);

        lang = new LangTranslation();
        lang.load();

        setGameScene(new MenuScene());
    }

    private void printComputerInfo() {
        log.info("Game version: {}", Constants.version);
        log.info("GLFW version: {}", glfwGetVersionString());

        // TODO упадёт когда доделаю оконный режим
        long monPtr = glfwGetPrimaryMonitor();
        if (monPtr != MemoryUtil.NULL) {
            int w, h;
            try (var stack = MemoryStack.stackPush()) {
                var wPtr = stack.mallocInt(1);
                var hPtr = stack.mallocInt(1);
                glfwGetMonitorPhysicalSize(monPtr, wPtr, hPtr);
                w = wPtr.get();
                h = hPtr.get();
            }
            log.info("Screen resolution: {}x{}", w, h);
        } else {
            // у меня на wayland такое возможно
            // не хочу это сейчас исправлять, поскольку есть планы как вывести тут важную информацию
        }

        // Это интел-специфичная штука
        if (Platform.get() == Platform.WINDOWS) {
            log.info("CPU: {}", System.getenv("PROCESSOR_IDENTIFIER"));
        }
        var memMxbean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        memMxbean.getCpuLoad();

        double gib = 1024d / 1024d / 1024d;
        log.info("Heap max capacity: {} GiB", DebugTools.FLOATS.format(Runtime.getRuntime().maxMemory() / gib));
        log.info("Total memory size: {} GiB", DebugTools.FLOATS.format(memMxbean.getTotalMemorySize() / gib));
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
