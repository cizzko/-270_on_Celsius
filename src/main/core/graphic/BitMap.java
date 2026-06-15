package core.graphic;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.lang.foreign.MemorySegment;

public record BitMap(int width, int height, MemorySegment data, int glFormat, int glType) implements NativeResource {
    @Override
    public void free() {
        MemoryUtil.nmemFree(data.address());
    }
}
