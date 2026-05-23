package core.graphic;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;

public record BitMap(int width, int height, ByteBuffer data) implements NativeResource {
    @Override
    public void free() {
        MemoryUtil.memFree(data);
    }
}
