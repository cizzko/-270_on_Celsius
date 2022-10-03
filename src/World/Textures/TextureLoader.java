package World.Textures;

import org.lwjgl.BufferUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.glOrtho;

public class TextureLoader {
    public static void loadTexture(String path){
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
        TextureDrawing.draw(image, buffer);
    }
}