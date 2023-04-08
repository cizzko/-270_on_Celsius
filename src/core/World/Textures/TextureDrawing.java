package core.World.Textures;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL13.*;

public class TextureDrawing {

    @Deprecated
    //not recommended since it is a pipeline, now all rendering goes in the shader
    public static void draw(String path, int x, int y) {
        glPushMatrix();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);;

        ByteBuffer buffer = TextureLoader.ByteBufferEncoder(path);
        BufferedImage image = TextureLoader.BufferedImageEncoder(path);

        // параметры, бинд текстур, и прочее
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getHeight(), image.getWidth(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        // верхний левый угол
        glBegin(GL_QUADS);
        glTexCoord2i(0, 1);
        glVertex2i(x, y);
        // верхний правый угол
        glTexCoord2i(1, 1);
        glVertex2i(image.getHeight() + x, y);
        // нижний правый угол
        glTexCoord2i(1, 0);
        glVertex2i(image.getHeight() + x, image.getWidth() + y);
        // нижний левый угол
        glTexCoord2i(0, 0);
        glVertex2i(x, image.getWidth() + y);

        //glVertex2i Задает вершины
        //glTexCoord2i Задает текущие координаты текстуры

        glEnd();
        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
    }
}