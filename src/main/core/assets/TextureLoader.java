package core.assets;

import core.Global;
import core.g2d.Font;
import core.Utils.SimpleColor;
import org.lwjgl.BufferUtils;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static core.EventHandling.Logging.Logger.*;
import static core.Window.*;

public class TextureLoader {
    public record ImageData(int width, int height, ByteBuffer data) {}
    public record GifImageData(int width, int height, ByteBuffer[] data) {}

    // returns BufferedImage target image
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
        // decodes the image into rgba and loads each byte into the buffer
        int BYTES_PER_PIXEL = 4;
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * BYTES_PER_PIXEL); // 4 bytes per pixel for RGBA, 3 for RGB

        // load pixels
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

    public static void preLoadResources() throws IOException {
        defaultFont = Font.load(Global.assets.assetsDir("UI/arial.ttf"));
    }

    // decode and returns gif object of .gif file
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
