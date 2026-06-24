package core.g2d;

import core.util.Disposable;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.glDeleteBuffers;

public final class ElementBufferObject implements Disposable {
    final int id;
    final IntBuffer buffer;

    ElementBufferObject(IntBuffer buffer) {
        this.id = OpenGL.createBuffer();
        this.buffer = buffer;
    }

    public int id() {
        return id;
    }

    public void bind() {
        OpenGL.bindBuffer(GL_ELEMENT_ARRAY_BUFFER, id);
    }

    public void upload(int usage) { // не забудь vao
        OpenGL.setBufferData(id, GL_ELEMENT_ARRAY_BUFFER, buffer, usage);
    }

    @Override
    public void close() {
        glDeleteBuffers(id);
        MemoryUtil.memFree(buffer);
    }
}
