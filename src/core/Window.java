package core;

import core.EventHandling.MouseScrollCallback;
import core.World.MainMenu;
import core.World.Textures.TextureLoader;
import core.World.WorldGenerator;
import core.World.WorldObjects;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL43.GL_DEBUG_OUTPUT;

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
        draw();
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

    public void draw() {
        glEnable(GL_DEBUG_OUTPUT);
        float cameraX = 1f;
        float cameraY = 1f;
        float zoom = 4f;
        boolean start = false;

        WorldGenerator.generateDynamicsObjects();
        WorldGenerator.generateStaticObjects(1000, 20);
        WorldObjects[][] objects = WorldGenerator.StaticObjects;

        glfwSwapBuffers(glfwWindow);
        glClear(GL_COLOR_BUFFER_BIT);


        // создаем вершинный шейдер
        String vertexShaderSource = "#version 330 core\n" +
                "layout(location = 0) in vec2 position;\n" +
                "layout(location = 1) in vec2 texCoord;\n" +
                "out vec2 fragTexCoord;\n" +
                "void main() {\n" +
                "    gl_Position = vec4(position, 0.0, 1.0);\n" +
                "    fragTexCoord = texCoord;\n" +
                "}";

        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);

        // создаем фрагментный шейдер
        String fragmentShaderSource = "#version 330 core\n" +
                "in vec2 fragTexCoord;\n" +
                "out vec4 fragColor;\n" +
                "uniform sampler2D texture;\n" +
                "void main() {\n" +
                "    fragColor = texture2D(texture, fragTexCoord);\n" +
                "}";

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);

        // создаем шейдерную программу
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        glUseProgram(program);

        // создаем массив вершин и текстурных координат
        float[] vertices = {
                -1, 1,  0, 0, // верхний левый угол
                1, 1,  1, 0, // верхний правый угол
                1, -1, 1, 1, // нижний правый угол
                -1, -1, 0, 1  // нижний левый угол
        };

        // создаем буфер вершин и загружаем данные
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();

        // создаем VAO и связываем буфер вершин
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
        glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        // устанавливаем uniform переменные
        int textureLoc = glGetUniformLocation(program, "texture");
        glUniform1i(textureLoc, 0);


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

                        if (objects[x][y].onCamera) {
                            ByteBuffer buffer = TextureLoader.ByteBufferEncoder(objects[x][y].path);
                            BufferedImage image = TextureLoader.BufferedImageEncoder(objects[x][y].path);

                            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
                            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

                            glPushMatrix();
                            glTranslatef(-cameraX, -cameraY, 0);
                            glScalef(zoom, zoom, 0);
                            glDrawArrays(GL_QUADS, 0, 4);
                            glPopMatrix();
                        }
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