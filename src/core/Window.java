package core;

import core.EventHandling.MouseScrollCallback;
import core.World.MainMenu;
import core.World.Textures.TextureLoader;
import core.World.WorldGenerator;
import core.World.WorldObjects;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.*;
import org.lwjgl.openvr.Texture;
import render.Draw;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL43.GL_DEBUG_OUTPUT;
import static render.Draw.compileShader;

public class Window {
    public final int width;
    public final int height;
    private final String title;
    public static long glfwWindow;
    private static Window window;
    private static Physics thread = new Physics();

    public Window() {
        //чекает размеры твоего экрана
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.width = dim.width;
        this.height = dim.height;
        this.title = "-270 on Celsius";
    }

    public static Window get() {
        //если окно не существует (null), то оно создается
        if (Window.window == null) {
            Window.window = new Window();
        }
        return Window.window;
    }

    public void run() {
        init();
        try {
            draw();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void init() {
        //инициализирует библиотеку
        glfwInit();
        System.out.println("'glfw' has been initialized");
        GLFWErrorCallback.createPrint(System.err).set();
        if (glfwWindow == NULL) {
            //если окна не существует - создаст
            glfwGetCurrentContext();
            glfwWindow = glfwCreateWindow(this.width, this.height, this.title, glfwGetPrimaryMonitor(), NULL);
        } else {
            //при попытке создания окна возникла ошибка - сообщит
            System.err.println("error at create glfwWindow");
        }
        glfwMakeContextCurrent(glfwWindow);
        //vsync
        glfwSwapInterval(1);
        //настройка отображения
        glfwShowWindow(glfwWindow);
        //подключает инструменты библиотеки
        GL.createCapabilities();
        System.out.println(glfwGetVersionString());

        glMatrixMode(GL_PROJECTION);
        glOrtho(0, width, 0, height, 1, -1);
        glMatrixMode(GL_MODELVIEW);

        MainMenu.Create();
        glfwSetScrollCallback(glfwWindow, new MouseScrollCallback());
    }

    public void draw() throws IOException {
        glfwSwapBuffers(glfwWindow);

        // Загрузка шейдеров
        int vertexShader = Draw.compileShader(GL_VERTEX_SHADER, Draw.loadShaderFromFile("D:\\-270_on_Celsius\\src\\render\\shaders\\vertex.glsl"));
        int fragmentShader = Draw.compileShader(GL_FRAGMENT_SHADER, Draw.loadShaderFromFile("D:\\-270_on_Celsius\\src\\render\\shaders\\fragment.glsl"));

        // Создание шейдерной программы
        int shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);

        // Привязка вершинных атрибутов
        glBindAttribLocation(shaderProgram, 0, "position");
        glBindAttribLocation(shaderProgram, 1, "textureCoords");

        // Компиляция шейдерной программы
        glLinkProgram(shaderProgram);
        glValidateProgram(shaderProgram);

        // Отрисовка текстуры
        Draw.drawTexture("D:\\-270_on_Celsius\\src\\assets\\TestImageForDrawing.png", 0, 0, shaderProgram);

        float cameraX = 1f;
        float cameraY = 1f;
        float zoom = 4f;
        boolean start = false;

        WorldGenerator.generateDynamicsObjects();
        WorldGenerator.generateStaticObjects(1000, 20);
        WorldObjects[][] objects = WorldGenerator.StaticObjects;

        glfwSwapBuffers(glfwWindow);


        //пока окно не закрыто
        while (!glfwWindowShouldClose(glfwWindow)) {
            //alt
            if(glfwGetKey(glfwWindow, 342) == 1){
                System.exit(0);
            }

            if (start) {
                glClear(GL_COLOR_BUFFER_BIT);
                for (int x = 0; x < objects.length - 1; x++) {
                    for (int y = 0; y < objects[x].length - 1; y++) {

                        float left = cameraX - width / zoom;
                        float right = cameraX + width / zoom;
                        float top = cameraY - height / zoom;
                        float bottom = cameraY + height / zoom;

                        if (objects[x][y].x < left || objects[x][y].x > right || objects[x][y].y < top || objects[x][y].y > bottom) {
                            objects[x][y].onCamera = false;
                        } else {
                            objects[x][y].onCamera = true;
                        }

                        if (glfwGetKey(glfwWindow, GLFW_KEY_1) == GLFW_PRESS) zoom += 0.000005f;
                        if (glfwGetKey(glfwWindow, GLFW_KEY_2) == GLFW_PRESS) zoom -= 0.000005f;
                        if (glfwGetKey(glfwWindow, GLFW_KEY_D) == GLFW_PRESS) cameraX += 0.0057f;
                        if (glfwGetKey(glfwWindow, GLFW_KEY_A) == GLFW_PRESS) cameraX -= 0.0057f;
                        if (glfwGetKey(glfwWindow, GLFW_KEY_W) == GLFW_PRESS) cameraY += 0.0057f;
                        if (glfwGetKey(glfwWindow, GLFW_KEY_S) == GLFW_PRESS) cameraY -= 0.0057f;

                    }
                }
                glfwSwapBuffers(glfwWindow);
            }
            //f1
            else if (glfwGetKey(glfwWindow, 290) == 1) {
                thread.start();
                start = true;
            }
            glfwPollEvents();
        }
    }
}