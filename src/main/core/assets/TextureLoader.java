package core.assets;

import core.Global;
import core.Utils.Color;
import core.g2d.BitMap;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import static core.EventHandling.Logging.Logger.*;

public class TextureLoader {
    public static BufferedImage readBufferedImage(String file) {
        try (var in = Files.newInputStream(Global.assets.assetsDir().resolve(file))) {
            return ImageIO.read(in);
        } catch (IOException e) {
            printException("Error at buffered image encoder, path: " + file, e);
            throw new UncheckedIOException(e);
        }
    }
    public static BitMap decodeImage(BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        ByteBuffer buffer = MemoryUtil.memAlloc(image.getWidth() * image.getHeight() * 4)
                .order(ByteOrder.BIG_ENDIAN); // BufferedImage не умеет в адекватное API. Почему не нативный порядок?

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = pixels[y * image.getWidth() + x];
                buffer.putInt(Color.argbToRgba8888(argb));
            }
        }

        return new BitMap(image.getWidth(), image.getHeight(), buffer.flip());
    }

    public record GifImageData(int width, int height, ByteBuffer[] data) {}

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
                    BufferedImage image = reader.read(index);
                    frames[index] = decodeImage(image).data();
                }
                return new GifImageData(reader.read(0).getWidth(), reader.read(0).getHeight(), frames);
            }
        } catch (IOException e) {
            printException("Error when decode texture: ", e);
        }
        return null;
    }
}
