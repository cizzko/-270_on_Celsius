package World.Textures;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glTexCoord2i;

public class TextureDrawing {
    public static void drawOnPath(String path, int x, int y, ByteBuffer buffer, BufferedImage image) {
        //если при вызове не приходят буфферы, то сам декодирует их исходя из пути
        if(buffer == null){
            System.err.println("drawOnPath: ByteBuffer is null!");
            buffer = TextureLoader.ByteBufferEncoder(path);
        }
        if(image == null) {
            System.err.println("drawOnPath: ImageBuffer is null!");
            image = TextureLoader.BufferedImageEncoder(path);
        }
        //считывает размеры экрана
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int ScreenWidth;
        int ScreenHeight;
        ScreenWidth = dim.width;
        ScreenHeight = dim.height;

        //генерация айдишника текстуры
        //int textureID = glGenTextures();

        glOrtho(0, ScreenHeight, ScreenWidth, 0, -1.0, 1.0);
        //параметры, бинд текстур, и прочее
        //glBindTexture(GL_TEXTURE_2D, textureID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        //очистка и рисовка квада на экране
        //РИСОВКА ОБЯЗАТЕЛЬНО ИДЕТ МЕЖДУ glBegin(); и glEnd();
        //разрешение рисовки/наложения текстур
        glEnable(GL_TEXTURE_2D);
        glBegin(GL_QUADS);

        //настройки отрисовки и позиций
        //положительный y - вниз, положительный x - вправо

        /*
                   -5
                   -4
                   -3
                   -2
                   -1
     -1 -2 -3 -4 -5 0 1  2  3  4  5 ▷ x
                    1
                    2
                    3
                    4
                    5
                    ▽
                    y

            да, мне было нечего делать
        */

        //верхний левый угол
        glTexCoord2i(0, 0);
        glVertex2i(y, x);
        //нижний левый угол
        glTexCoord2i(0, 1);
        glVertex2i(0 + y, image.getWidth() + x);
        //нижний правый угол
        glTexCoord2i(1, 1);
        glVertex2i(image.getHeight() + y, image.getWidth() + x);
        //верхний правый угол
        glTexCoord2i(1, 0);
        glVertex2i(image.getHeight() + y, 0 + x);

        //glVertex2i Задает вершины
        //glTexCoord2i Задает текущие координаты текстуры, понятия не имею как работает, попробуй поизменять
        glEnd();
        glDisable(GL_TEXTURE_2D);
        //чистит буффер, что бы следующая отрисовка была верной, временно закоменчен
        //buffer.clear();
    }
    public static void drawOnByteBuff(ByteBuffer buffer, int x, int y){

    }
    public static void drawOnHitBox(String path, int x, int y){

    }
}
