package core.g2d;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;

public final class BitMap implements NativeResource {
    public final int width;
    public final int height;
    public final ByteBuffer data;

    private boolean released;

    public BitMap(int width, int height, ByteBuffer data) {
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public ByteBuffer data() {
        return data;
    }

    @Override
    public String toString() {
        return "BitMap[" +
                "width=" + width + ", " +
                "height=" + height + ", " +
                "data=" + data + ']';
    }

    @Override
    public void free() {
        if (released) {
            return;
        }
        released = true;
        MemoryUtil.memFree(data);
    }

}
