package core.graphic;

import core.g2d.BitMap;
import core.util.Color;
import org.lwjgl.system.MemoryUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TextureLoader {

    public static BitMap decodeImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4)
                .order(ByteOrder.BIG_ENDIAN); // BufferedImage не умеет в адекватное API. Почему не нативный порядок?

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = pixels[y * width + x];
                buffer.putInt(Color.argbToRgba8888(argb));
            }
        }

        return new BitMap(width, height, buffer.flip());
    }
}
