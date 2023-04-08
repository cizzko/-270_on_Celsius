package render;

import core.World.Textures.TextureDrawing;
import core.World.Textures.TextureLoader;
import core.World.WorldObjects;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL43.GL_DEBUG_OUTPUT;
import static org.lwjgl.opengl.GL43.glDebugMessageCallback;

public class Draw {
    public static String loadShaderFromFile(String filePath) throws IOException {
        StringBuilder shaderSource = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null) {
            shaderSource.append(line).append("\n");
        }
        reader.close();
        return shaderSource.toString();
    }

    public static int compileShader(int shaderType, String shaderSource) {
        // Создание шейдера
        int shader = glCreateShader(shaderType);
        int[] success = new int[4];
        int length = 8192;

        // Загрузка шейдера
        glShaderSource(shader, shaderSource);

        // Компиляция шейдера
        glCompileShader(shader);

        // Проверка наличия ошибок компиляции
        glGetShaderiv(shader, GL_COMPILE_STATUS, success);

        if (success[0] == GL_FALSE) {
            System.err.println(glGetShaderInfoLog(shader, length) + ", shader type: " + shaderType + ", shader source: " + shaderSource);
            glDeleteShader(shader);

            return 0;
        }

        return shader;
    }


    public static void drawTexture(WorldObjects[][] StaticObjects, WorldObjects[] DynamicObjects, int shader) {

    }

    public static void drawTexture(String path, int x, int y, int shader) {
        int textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        BufferedImage image = TextureLoader.BufferedImageEncoder(path);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, TextureLoader.ByteBufferEncoder(path));

        float[] vertices = {
                // Положение вершин (x + x_offset, y + y_offset) и координаты текстуры (s, t)
                0.5f + x, 0.5f + y, 1.0f, 1.0f,  // Верхний правый угол
                0.5f + x, -0.5f + y, 1.0f, 0.0f,  // Нижний правый угол
                -0.5f + x, -0.5f + y, 0.0f, 0.0f,  // Нижний левый угол
                -0.5f + x, 0.5f + y, 0.0f, 1.0f   // Верхний левый угол
        };

        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // Создание буфера вершин и связывание с контекстом OpenGL
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // Загрузка данных вершин в буфер
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // Создание EBO и связывание с контекстом OpenGL
        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        // Загрузка индексных данных в буфер
        glUseProgram(shader);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);

        glBindVertexArray(vao);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        int textureLocation = glGetUniformLocation(shader, "myTexture");
        glUniform1i(textureLocation, 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    public static void drawEffect(String type, int x, int y, int time, int radius) {

    }
}