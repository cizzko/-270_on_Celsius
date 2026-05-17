package core.g2d;

import core.util.Disposable;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15C.*;

public final class ElementBufferObject implements Disposable {
    final int id;
    final IntBuffer buffer;

    ElementBufferObject(IntBuffer buffer) {
        this.id = glGenBuffers();
        this.buffer = buffer;
    }

    public int id() {
        return id;
    }

    public void bind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id);
    }

    public void upload(int usage) { // не забудь vao
        bind();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, usage);
    }

    @Override
    public void close() {
        glDeleteBuffers(id);
        MemoryUtil.memFree(buffer);
    }
}
