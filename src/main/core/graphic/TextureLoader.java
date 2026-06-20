package core.graphic;

import core.math.MathUtil;
import org.lwjgl.system.MemoryUtil;

import java.awt.image.BufferedImage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static org.lwjgl.opengl.GL46C.*;

public final class TextureLoader {
    private TextureLoader() {}

    public static BitMap decodeImage(BufferedImage image) {
        int imageType = image.getType();
        if (imageType != TYPE_4BYTE_ABGR && imageType != TYPE_INT_ARGB)
            throw new IllegalArgumentException("Image type is not ABGR");

        int width = image.getWidth();
        int height = image.getHeight();

        short wU16 = MathUtil.toShortExact(width);
        short wV16 = MathUtil.toShortExact(height);

        var pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        long byteSize = (long)width * height * Integer.BYTES;
        long ptr = MemoryUtil.nmemAlignedAllocChecked(Integer.BYTES, byteSize);
        var segment = MemorySegment.ofAddress(ptr)
                .reinterpret(byteSize);

        int glFormat;
        int glType;

        for (int y = 0; y < height; y++) {
            int line = y * width;
            for (int x = 0; x < width; x++) {
                int idx = line + x;
                int argb = pixels[idx];
                segment.setAtIndex(ValueLayout.JAVA_INT, idx,
                        Integer.reverseBytes(Color.argbToRgba8888(argb)));
            }
        }

        if (imageType == BufferedImage.TYPE_INT_ARGB) {
            glFormat = GL_RGBA;
            glType = GL_UNSIGNED_INT_8_8_8_8_REV;
        } else {
            glFormat = GL_RGBA;
            glType = GL_UNSIGNED_INT_8_8_8_8_REV;
        }

        return new BitMap(wU16, wV16, segment, glFormat, glType);
    }
}
