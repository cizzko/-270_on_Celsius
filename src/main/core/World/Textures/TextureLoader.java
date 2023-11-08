package core.World.Textures;

import core.EventHandling.Logging.Config;
import core.UI.GUI.Fonts;
import core.Utils.ArrayUtils;
import org.lwjgl.BufferUtils;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import static core.EventHandling.Logging.Logger.*;
import static core.UI.GUI.Fonts.getCharBuffer;
import static core.UI.GUI.Fonts.letterSize;
import static core.Window.assetsDir;
import static core.Window.pathTo;
import static core.World.Textures.TextureDrawing.bindTexture;
import static org.lwjgl.opengl.GL11.*;

public class TextureLoader extends Thread {
    //private static SoftReference<HashMap<String, Size>> sizes = new SoftReference<>(new HashMap<>());
    private static HashMap<String, Size> sizes = new HashMap<>();
    public record Size(int width, int height) {}
    public record ImageData(int width, int height, ByteBuffer data) {}

    public static BufferedImage BufferedImageEncoder(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            printException("Error at buffered image encoder, path: " + path, e);
            logExit(1);
        }
        return null;
    }

    public static ImageData readImage(BufferedImage image) {
        //decodes the image into rgba and loads each byte into the buffer
        int BYTES_PER_PIXEL = 4;
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * BYTES_PER_PIXEL); //4 bytes per pixel for RGBA, 3 for RGB

        //load pixels
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));     // red
                buffer.put((byte) ((pixel >> 8) & 0xFF));      // green
                buffer.put((byte) (pixel & 0xFF));             // blue
                buffer.put((byte) ((pixel >> 24) & 0xFF));     // alpha, used in RGBA and provides the degree of transparency of a pixel.
            }
        }

        return new ImageData(image.getWidth(), image.getHeight(), buffer.flip());
    }

    public static ByteBuffer ByteBufferEncoder(String path) {
        return readImage(BufferedImageEncoder(path)).data;
    }

    public static ByteBuffer ByteBufferEncoder(BufferedImage image) {
        return readImage(image).data;
    }

    public static Size getSize(String path) {
        path += path.endsWith(".png") ? "" : "1.png";
        return getSizeStatic(path);
    }

//    TODO: тестовая конструкция
//    public static Size getSizeStatic(String path) {
//        HashMap<String, Size> ref = sizes.get();
//
//        if (ref == null) {
//            ref = new HashMap<>();
//            BufferedImage encoder = BufferedImageEncoder(path);
//            Size size = new Size(encoder.getWidth(), encoder.getHeight());
//            ref.put(path, size);
//            sizes = new SoftReference<>(ref);
//
//            return size;
//        } else if (ref.get(path) != null) {
//            return ref.get(path);
//        } else {
//            BufferedImage encoder = BufferedImageEncoder(path);
//            Size size = new Size(encoder.getWidth(), encoder.getHeight());
//            ref.put(path, size);
//            sizes = new SoftReference<>(ref);
//
//            return size;
//        }
//    }

    public static Size getSizeStatic(String path) {
        if (sizes.get(path) != null) {
            return sizes.get(path);
        } else {
            BufferedImage encoder = BufferedImageEncoder(path);
            Size size = new Size(encoder.getWidth(), encoder.getHeight());
            sizes.put(path, size);
            return size;
        }
    }

    public static ByteBuffer uniteTextures(String mainTexture, String secondTexture) {
        BufferedImage mergedImage = BufferedImageEncoder(mainTexture);
        Graphics2D g2d = mergedImage.createGraphics();

        g2d.drawImage(BufferedImageEncoder(secondTexture), 0, 0, null);
        g2d.dispose();

        return ByteBufferEncoder(mergedImage);
    }

    public static void preLoadResources() {
        Fonts.generateFont(assetsDir("UI/arial.ttf"));
        if (Config.getFromConfig("PreloadResources").equals("true")) {
            String[] textures = ArrayUtils.getAllFiles(pathTo("src/assets"), ".png");

            for (String texture : textures) {
                bindTexture(texture);
            }

            log("Texture loader: load '" + textures.length + "' textures");
            bindChars();
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
            TextureDrawing.textures.put(character.hashCode(), id);

            glBindTexture(GL_TEXTURE_2D, 0);
        });
    }
}
