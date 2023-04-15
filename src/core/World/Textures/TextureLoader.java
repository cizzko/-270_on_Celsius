package core.World.Textures;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class TextureLoader extends Thread {
    static ConcurrentHashMap<String, BufferedImage> bufferedImages = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, ByteBuffer> byteBuffers = new ConcurrentHashMap<>();

    //не может вернуть null значение
    @NotNull
    public static BufferedImage BufferedImageEncoder(String path) {
        //если картинка не создана - пытается создать, иначе выдаст ошибку
        if (bufferedImages.get(path) == null) {
            try {
                bufferedImages.put(path, ImageIO.read(new File(path)));
            } catch (Exception e) {
                System.err.println("Critical err at BufferedImageEncoder'" + e + "', Path '" + path + "'");
                //команда выхода из программы
                System.exit(0);
            }
        }
        //декодируется картинка - вернется буффер картинки
        return bufferedImages.get(path);
    }


    @NotNull
    public static ByteBuffer ByteBufferEncoder(String path) {
        ByteBuffer buffer;
        BufferedImage image = BufferedImageEncoder(path);

        if (byteBuffers.get(path) == null) {
            //декодирует картинку в ргба, и загружает каждый байт в буффер
            int BYTES_PER_PIXEL = 4;
            int[] pixels = new int[image.getWidth() * image.getHeight()];
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
            buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * BYTES_PER_PIXEL); //4 байта на пиксель для ргба, 3 под ргб

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
            //я вам запрещаю забывать про эту строчку
            buffer.flip();
            byteBuffers.put(path, buffer);
        }
        //декодируется в буффер, его и вернет
        return byteBuffers.get(path);
    }
}