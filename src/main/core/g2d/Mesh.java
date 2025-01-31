package core.g2d;

import core.Utils.Disposable;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL46.*;

public final class Mesh implements Disposable {

    private final int vaoUsage;
    private final int vao, vbo, ebo;

    private boolean useIndexes;
    private boolean disposed;

    public Mesh(int vaoUsage) {
        this.vaoUsage = vaoUsage;

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();
    }

    public void updateIndexes(IntBuffer indexes) {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexes, vaoUsage);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void useIndexes(boolean state) {
        this.useIndexes = state;
    }

    public void bindVbo() {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
    }

    private void bindEbo() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
    }

    public void bindVao() {
        glBindVertexArray(vao);
    }

    public void draw(int primitiveType, FloatBuffer vertices, int vertexCount) {
        glBindVertexArray(vao);

        if (vertexCount > 0) {
            bindVbo();
            glBufferData(GL_ARRAY_BUFFER, vertices, vaoUsage);
        }

        if (useIndexes) {
            bindEbo();
            glDrawElements(primitiveType, vertexCount, GL_UNSIGNED_INT, 0L);
        } else {
            glDrawArrays(primitiveType, 0, vertexCount);
        }

        glBindVertexArray(0);
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void close() {
        if (disposed) {
            return;
        }

        try (var stack = MemoryStack.stackPush()) {
            var ids = stack.ints(vbo, ebo);
            glDeleteBuffers(ids);
        }
        glDeleteVertexArrays(vao);

        disposed = true;
    }
}
