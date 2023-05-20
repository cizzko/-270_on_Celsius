package core;

import core.EventHandling.MouseScrollCallback;
import core.UI.GUI.CreateElement;
import core.UI.GUI.Fonts;
import core.UI.GUI.Video;
import core.EventHandling.Logging.config;
import core.EventHandling.Logging.logger;
import core.UI.GUI.Menu.MainMenu;
import core.World.WorldGenerator;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import java.nio.file.Paths;
import static core.World.Textures.TextureDrawing.*;
import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;

public class Window {
    public static int width, height, deltaTime;
    public static final String title = "-270 on Celsius", version = "dev 0.0.4";
    public static String defPath = Paths.get("").toAbsolutePath().toString();
    public static boolean start = false, fullScreen = Boolean.parseBoolean(config.jetFromConfig("FullScreen"));
    public static long glfwWindow, lastFrameTime = System.currentTimeMillis();

    public Window() {
        width = Integer.parseInt(config.jetFromConfig("ScreenWidth"));
        height = Integer.parseInt(config.jetFromConfig("ScreenHeight"));
    }

    public void run() {
        init();
        draw();
    }

    public void init() {
        //инициализирует библиотеку
        glfwInit();
        glfwGetCurrentContext();
        GLFWErrorCallback.createPrint(System.err).set();
        if (glfwWindow == NULL) {
            //если окна не существует - создаст
            if (fullScreen) {
                glfwWindow = glfwCreateWindow(width, height, title, glfwGetPrimaryMonitor(), NULL);
            }
            else {
                glfwWindow = glfwCreateWindow(width, height, title, NULL, NULL);
            }
            glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        } else {
            //при попытке создания окна возникла ошибка - сообщит
            logger.log("error at create window");
        }
        glfwMakeContextCurrent(glfwWindow);
        glfwSetScrollCallback(glfwWindow, new MouseScrollCallback());
        //vsync
        glfwSwapInterval(1);
        //настройка отображения
        glfwShowWindow(glfwWindow);
        //подключает инструменты библиотеки
        GL.createCapabilities();

        glMatrixMode(GL_PROJECTION);
        glOrtho(0, width, 0, height, 1, -1);
        glMatrixMode(GL_MODELVIEW);

        Fonts.generateFont(defPath + "\\src\\assets\\GUI\\arial.ttf");
        Video.drawVideo(defPath + "\\src\\assets\\World\\kaif.mp4", 1, 30, 0, 0, 1920, 1080);
        MainMenu.create();

        logger.logStart();
    }

    public void draw() {
        new Thread(new CreateElement()).start();

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
        }
    }
}