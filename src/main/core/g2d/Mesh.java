package core.g2d;

import core.Utils.Disposable;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL46.*;

public final class Mesh implements Disposable {

    private final int vaoUsage;
    private final int vao, vbo, eab;

    private IntBuffer indexes;

    private boolean disposed;

    public Mesh(int vaoUsage) {
        this.vaoUsage = vaoUsage;

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        eab = glGenBuffers();
    }

    public void updateIndexes(IntBuffer indexes) {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eab);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexes, vaoUsage);

        this.indexes = indexes;
    }

    public void bindVbo() {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
    }

    public void render(int primitiveType, FloatBuffer vertices, int vertexCount) {
        if (vertexCount > 0) {
            bindVbo();
            glBufferData(GL_ARRAY_BUFFER, vertices, vaoUsage);
        }

        if (indexes != null) {
            indexes.rewind();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eab);

            glDrawElements(primitiveType, vertexCount, GL_UNSIGNED_INT, 0L);
        } else {
            glDrawArrays(primitiveType, 0, vertexCount);
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void bindVao() {
        glBindVertexArray(vao);
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void close() {
        if (disposed) return;
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(eab);
        disposed = true;
    }
}
