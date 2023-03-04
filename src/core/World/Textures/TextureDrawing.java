package core.World.Textures;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL13.*;

public class TextureDrawing {
    public static void draw(String path, int x, int y, ByteBuffer buffer, BufferedImage image) {
        glEnable(GL_TEXTURE_2D);
        //если при вызове не приходят буфферы, то сам декодирует их исходя из пути
        if (buffer == null) {
            System.err.println("buffer is null");
            buffer = TextureLoader.ByteBufferEncoder(path);
        }
        if (image == null) {
            System.err.println("image is null");
            image = TextureLoader.BufferedImageEncoder(path);
        }
        //параметры, бинд текстур, и прочее
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getHeight(), image.getWidth(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        //очистка и рисовка квада на экране
        //РИСОВКА ОБЯЗАТЕЛЬНО ИДЕТ МЕЖДУ glBegin(); и glEnd();
        //разрешение рисовки/наложения текстур
        glBegin(GL_QUADS);
        //верхний левый угол
        glTexCoord2i(0, 0);
        glVertex2i(x, y);
        //нижний левый угол
        glTexCoord2i(0, 1);
        glVertex2i(x, image.getWidth() + y);
        //нижний правый угол
        glTexCoord2i(1, 1);
        glVertex2i(image.getHeight() + x, image.getWidth() + y);
        //верхний правый угол
        glTexCoord2i(1, 0);
        glVertex2i(image.getHeight() + x, y);

        //glVertex2i Задает вершины
        //glTexCoord2i Задает текущие координаты текстуры

        glEnd();
        glDisable(GL_TEXTURE_2D);
    }
}