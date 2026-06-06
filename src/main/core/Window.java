package core;

import com.sun.management.OperatingSystemMXBean;
import core.EventHandling.Config;
import core.assets.AssetsManager;
import core.content.EntityPool;
import core.g2d.*;
import core.input.InputHandler;
import core.util.Debug;
import core.util.FutureUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.io.IoBuilder;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;

import static core.Global.*;
import static core.graphic.TextureLoader.decodeImage;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public final class Window extends Application {
    private static final Logger lwjglLogger = LogManager.getLogger("LWJGL");

    public static int defaultWidth = 1920, defaultHeight = 1080;
    public static boolean defaultFullscreen = true;

    public static boolean windowFocused = true;
    public static long glfwWindow;
    public static Font defaultFont;

    public static void setClipboardText(@Nullable CharSequence text) {
        glfwSetClipboardString(glfwWindow, text);
    }

    public static @Nullable String getClipboardText() {
        return glfwGetClipboardString(glfwWindow);
    }

    @Override
    protected void init() throws Throwable {
        // Хмм, надо бы где-то тут создавать сцену
        assets.load(Font.class, "arial.ttf");

        if (Debug.debugLevel >= 4) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_STREAM.set(IoBuilder.forLogger(lwjglLogger)
                    .setLevel(Level.DEBUG)
                    .buildPrintStream());
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
        }

       glfwSetErrorCallback(keep(new GLFWErrorCallback() {
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

        // glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11);
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
        // glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        if (Config.getBoolean("DebugMACOSX") || Platform.get() == Platform.MACOSX) {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        }

        glfwWindow = glfwCreateWindow(defaultWidth, defaultHeight, "-270 on Celsius",
                defaultFullscreen ? glfwGetPrimaryMonitor() : MemoryUtil.NULL, MemoryUtil.NULL);
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

        if (Config.getBoolean("VerticalSync")) {
            log.info("Target Framerate: Vertical Sync");
            glfwSwapInterval(1);
        } else {
            glfwSwapInterval(0);
            int targetFPS = Config.getInt("TargetFPS", -1);
            if (targetFPS != -1) {
                log.info("Target Framerate: {} FPS", targetFPS);
                setFramerate(targetFPS);
            } else {
                log.info("Target Framerate: Uncapped");
            }
        }

        GL.createCapabilities();

         if (Debug.debugLevel >= 5) {
             glEnable(GL_DEBUG_OUTPUT);
             keep(GLUtil.setupDebugMessageCallback());
             keep(() -> glDisable(GL_DEBUG_OUTPUT));
         }

        uiScene = new UIScene(defaultWidth, defaultHeight);
        input = new InputHandler(defaultWidth, defaultHeight);
        input.init();
        input.addListener(uiScene);

        glfwSetWindowFocusCallback(glfwWindow, keep(new GLFWWindowFocusCallback() {
            @Override
            public void invoke(long window, boolean focused) {
                windowFocused = focused;
            }
        }));
        glfwSetWindowCloseCallback(glfwWindow, keep(new GLFWWindowCloseCallback() {
            @Override
            public void invoke(long window) {
                quit();
            }
        }));

        assets.load(Atlas.class, "sprites", AssetsManager.LoadType.SYNC);
        Shaders.repeat = FutureUtil.join(assets.load(Shader.class, "repeat", AssetsManager.LoadType.SYNC));
        StackfulRender.defaultShader = FutureUtil.join(assets.load(Shader.class, "default", AssetsManager.LoadType.SYNC));
        Render.init();

        glClearColor(206f / 255f, 246f / 255f, 1.0f, 1.0f);

        glfwShowWindow(glfwWindow);

        lang = new LangTranslation();
        lang.load(); // TODO придумать как загружать и перезагружать

        entityPool = new EntityPool(Constants.Entity.MAX_COUNT);

        Debug.initDebugValuesMenu();

        setGameScene(new MenuScene());
    }

    private void printComputerInfo() {
        log.info("Game version: {}", Constants.version);
        log.info("GLFW version: {}", glfwGetVersionString());

        // TODO упадёт когда доделаю оконный режим
        long monPtr = glfwGetPrimaryMonitor();
        if (monPtr != MemoryUtil.NULL) {
            GLFWVidMode vidmode = glfwGetVideoMode(monPtr);

            if (vidmode != null) {
                int w = vidmode.width();
                int h = vidmode.height();

                log.info("Screen resolution: {}x{}", w, h);
            }
        }

        // Это интел-специфичная штука
        if (Platform.get() == Platform.WINDOWS) {
            log.info("CPU: {}", System.getenv("PROCESSOR_IDENTIFIER"));
        }
        var memMxbean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        double gib = 1024d * 1024d * 1024d;

        log.info("Heap max capacity: {} GiB", Debug.FLOATS.format(Runtime.getRuntime().maxMemory() / gib));
        log.info("Total memory size: {} GiB", Debug.FLOATS.format(memMxbean.getTotalMemorySize() / gib));
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
        // 6) Отрисовка мира в порядке отображения
        updateTime();

        input.update();
        var rq = Render.queue();
        rq.beginFrame();
        gameScene.loop();
        StackfulRender.pushRenderList();
        rq.endFrame();
        swapBuffers();

        nextFrame();
    }

    private void swapBuffers() {
        glfwSwapBuffers(glfwWindow);
        glClear(GL_COLOR_BUFFER_BIT);
    }

    @Override
    protected void cleanup() {
        glfwTerminate();
        Render.queue.close();
        assets.unloadAll();
    }
}
