package core.World.Textures;

import core.EventHandling.Logging.Config;
import core.Utils.ArrayUtils;
import org.lwjgl.BufferUtils;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import static core.EventHandling.Logging.Logger.*;
import static core.UI.GUI.Fonts.getCharBuffer;
import static core.UI.GUI.Fonts.letterSize;
import static core.Window.defPath;
import static core.World.Textures.TextureDrawing.bindTexture;
import static org.lwjgl.opengl.GL11.*;

public class TextureLoader extends Thread {
    private static final HashMap<String, Dimension> sizes = new HashMap<>();

    public static BufferedImage BufferedImageEncoder(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            printException("Error at buffered image encoder, path: " + path, e);
            logExit(1);
        }

        return null;
    }

    public static ByteBuffer ByteBufferEncoder(String path) {
        ByteBuffer buffer;
        BufferedImage image = BufferedImageEncoder(path);

        //декодирует картинку в ргба, и загружает каждый байт в буффер
        int BYTES_PER_PIXEL = 4;
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * BYTES_PER_PIXEL); //4 байта на пиксель для ргба, 3 под ргб

        //загрузка пикселей
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));     // красный компонент картинки
                buffer.put((byte) ((pixel >> 8) & 0xFF));      // зеленый компонент картинки
                buffer.put((byte) (pixel & 0xFF));             // синий компонент картинки
                buffer.put((byte) ((pixel >> 24) & 0xFF));     // альфа компонент, используется в ргба и обозначает степень прозрачности пикселя
            }
        }

        return buffer.flip();
    }

    public static ByteBuffer ByteBufferEncoder(BufferedImage image) {
        ByteBuffer buffer;

        //декодирует картинку в ргба, и загружает каждый байт в буффер
        int BYTES_PER_PIXEL = 4;
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * BYTES_PER_PIXEL); //4 байта на пиксель для ргба, 3 под ргб

        //загрузка пикселей
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));     // красный компонент картинки
                buffer.put((byte) ((pixel >> 8) & 0xFF));      // зеленый компонент картинки
                buffer.put((byte) (pixel & 0xFF));             // синий компонент картинки
                buffer.put((byte) ((pixel >> 24) & 0xFF));     // альфа компонент, используется в ргба и обозначает степень прозрачности пикселя
            }
        }

        return buffer.flip();
    }

    public static Dimension getSize(String path) {
        path += path.endsWith(".png") ? "" : "1.png";

        if (sizes.get(path) == null) {
            sizes.put(path, new Dimension(BufferedImageEncoder(path).getWidth(), BufferedImageEncoder(path).getHeight()));
        }
        return sizes.get(path);
    }

    public static ByteBuffer uniteTextures(String mainTexture, String secondTexture) {
        BufferedImage mergedImage = BufferedImageEncoder(mainTexture);
        Graphics2D g2d = mergedImage.createGraphics();

        g2d.drawImage(BufferedImageEncoder(secondTexture), 0, 0, null);
        g2d.dispose();

        return ByteBufferEncoder(mergedImage);
    }

    public static void preLoadResources() {
        if (Config.getFromConfig("PreloadResources").equals("true")) {
            String[] textures = ArrayUtils.getAllFiles(defPath + "\\src\\assets", ".png");

            for (String texture : textures) {
                bindTexture(texture);
            }

            log("Texture loader: load '" + textures.length + "' textures");
        }
    }

    public static void bindChars() {
        letterSize.forEach((character, dimension) -> {
            int width = dimension.width;
            int height = dimension.height;

            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, getCharBuffer(character));
            TextureDrawing.textures.put(character.hashCode(), new TextureData(id, width, height));

            glBindTexture(GL_TEXTURE_2D, 0);
        });
    }
}