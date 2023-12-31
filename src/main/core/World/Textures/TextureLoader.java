package core.World.Textures;

import core.EventHandling.Logging.Config;
import core.UI.GUI.Fonts;
import core.Utils.ArrayUtils;
import core.Utils.SimpleColor;
import org.lwjgl.BufferUtils;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import static core.EventHandling.Logging.Logger.*;
import static core.Window.*;
import static core.World.Textures.TextureDrawing.bindChars;
import static core.World.Textures.TextureDrawing.bindTexture;

public class TextureLoader extends Thread {
    private static HashMap<String, Size> sizes = new HashMap<>();

    public record Size(int width, int height) {}
    public record ImageData(int width, int height, ByteBuffer data) {}
    public record GifImageData(int width, int height, ByteBuffer[] data) {}

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
                int color = pixels[y * image.getWidth() + x]; // argb
                buffer.putInt(SimpleColor.argbToRgba(color));
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
        return getSizeStatic(path);
    }

    public static Size getSizeStatic(String path) {
        Size size = sizes.getOrDefault(path, null);
        if (size == null) {
            BufferedImage encoder = BufferedImageEncoder(path);
            size = new Size(encoder.getWidth(), encoder.getHeight());
            sizes.put(path, size);
        }
        return size;
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

    public static GifImageData framesDecoder(String path) {
        try {
            if (path.endsWith(".gif")) {
                ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
                File input = new File(path);
                ImageInputStream stream = ImageIO.createImageInputStream(input);
                reader.setInput(stream);

                ByteBuffer[] frames = new ByteBuffer[reader.getNumImages(true)];
                for (int index = 0; index < frames.length; index++) {
                    frames[index] = ByteBufferEncoder(reader.read(index));
                }
                return new GifImageData(reader.read(0).getWidth(), reader.read(0).getHeight(), frames);
            }
        } catch (IOException e) {
            printException("Error when decode texture: ", e);
        }
        return null;
    }
}