package Textures;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.openvr.Texture;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

public class TextureLoader {
    public static ByteBuffer loadTexture(String path, int widthT, int heightT) {

        //считывание размеров окна, возможно потом уберу
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int ScreenWidth;
        int ScreenHeight;
        ScreenWidth = dim.width;
        ScreenHeight = dim.height;

        System.out.println("'textureLoader' method has been initialized");
        BufferedImage image = null;
        //если картинка не создана - пытается создать
        if (image == null) {
            try {
                image = ImageIO.read(new File(path));
            } catch (Exception e) {
                System.out.println("Critical err '" + e + "', Path '" + path + "'");
                System.exit(0);
            }
            System.out.println("Image loaded: " + path);
        }

        //декодирует картинку в ргба, и загружает каждый байт в буффер
        int BYTES_PER_PIXEL = 4;
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * BYTES_PER_PIXEL); //4 for RGBA, 3 for RGB

        //вложенный цикл для загрузки каждого пикселя
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));     // красный компонент картинки
                buffer.put((byte) ((pixel >> 8) & 0xFF));      // зеленый компонент картинки
                buffer.put((byte) (pixel & 0xFF));             // синий компонент картинки
                buffer.put((byte) ((pixel >> 24) & 0xFF));     // альфа компонент, используется в ргба и обозначает степень прозрачности пикселя
            }
        }
        buffer.flip(); //FOR THE LOVE OF GOD DO NOT FORGET THIS
        //генерация айдишника текстуры
        int textureID = glGenTextures();
        //три строчки ниже настройка камеры, пояснение каждой буквы скину по надобности в дс
        glMatrixMode(GL_PROJECTION);
        glOrtho(ScreenWidth/2, ScreenWidth/2, ScreenHeight/2, ScreenHeight/2, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        //параметры, бинд текстур, и прочее
        glBindTexture(GL_TEXTURE_2D, textureID);
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
        //настройки отрисовки и позиций, за разбором тоже в дс
        glTexCoord2i(0, 0); glVertex2i(0, 0);
        glTexCoord2i(0, 1); glVertex2i(0, ScreenWidth);
        glTexCoord2i(1, 1); glVertex2i(ScreenHeight, ScreenWidth);
        glTexCoord2i(1, 0); glVertex2i(ScreenHeight, 0);

        glEnd();
        glDisable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);

        return buffer;

    }
    //перегрузка (по сути уже не нужна)
    public static ByteBuffer loadTexture(String path) {
        return loadTexture(path, 0, 0);
    }
}