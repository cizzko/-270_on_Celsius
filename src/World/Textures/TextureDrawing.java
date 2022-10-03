package World.Textures;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glTexCoord2i;

public class TextureDrawing {
    public static void draw(BufferedImage image, ByteBuffer buffer) {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int ScreenWidth;
        int ScreenHeight;
        ScreenWidth = dim.width;
        ScreenHeight = dim.height;

        //генерация айдишника текстуры
        int textureID = glGenTextures();
        glOrtho(0, ScreenHeight, ScreenWidth, 0, -1.0, 1.0);
        //параметры, бинд текстур, и прочее
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glBindTexture(GL_TEXTURE_2D, 0);

        //очистка и рисовка квада на экране
        glClear(GL_COLOR_BUFFER_BIT);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glEnable(GL_TEXTURE_2D);
        glBegin(GL_QUADS);

        //настройки отрисовки и позиций
        //верхний левый угол
        glTexCoord2i(0, 0);
        glVertex2i(0, 0);
        //нижний левый угол
        glTexCoord2i(0, 1);
        glVertex2i(0, image.getWidth());
        //нижний правый угол
        glTexCoord2i(1, 1);
        glVertex2i(image.getHeight(), image.getWidth());
        //верхний правый угол
        glTexCoord2i(1, 0);
        glVertex2i(image.getHeight(), 0);

        //glVertex2i Задает вершины
        //glTexCoord2i Задает текущие координаты текстуры

        glEnd();
        glDisable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        buffer.clear();
    }
}
