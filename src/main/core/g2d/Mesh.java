package core.g2d;

import core.util.Debug;
import core.util.Disposable;
import org.jetbrains.annotations.Nullable;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.memAddress;

public final class Mesh implements Disposable {

    static int lastVbo = -1, lastVao = -1;

    private final int vao, vbo;

    private boolean dirty = true;

    public Mesh() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
    }

    public void setDirty(boolean state) { dirty = state; }

    public void bindVbo() {
        if (lastVbo == vbo) {
            return;
        }
        lastVbo = vbo;
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
    }

    public void bindVao() {
        if (lastVao == vao) {
            return;
        }
        lastVao = vao;
        glBindVertexArray(vao);
    }

    public void vboUpload(IntBuffer buffer) { // не забудь vao
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
    }

    public boolean draw(int primitiveType,
                        IntBuffer vertices, int vertexOffset, int vertexCount,
                        @Nullable ElementBufferObject ebo, int indexOffset, int indexCount,
                        VertexFormat format) {

        if (vertexCount == 0) {
            return false;
        }

        bindVao();
        bindVbo();

        if (dirty) {
            int stride = format.vertexByteSize();
            int byteOffset = vertexOffset * stride;
            int byteSize = vertexCount * stride;
            int oldPos = vertices.position();
            int oldLim = vertices.limit();

            vertices.position(byteOffset / Float.BYTES);
            vertices.limit((byteOffset + byteSize) / Float.BYTES);
            try {
                nglBufferSubData(GL_ARRAY_BUFFER, byteOffset, byteSize, memAddress(vertices));
            } finally {
                vertices.position(oldPos);
                vertices.limit(oldLim);
            }
        }

        if (Debug.debugMesh) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }
        if (ebo != null && primitiveType != GL_TRIANGLE_STRIP) {
            ebo.bind();
            long byteOffsetInEBO = (long) indexOffset * Integer.BYTES;
            glDrawElements(primitiveType, indexCount, GL_UNSIGNED_INT, byteOffsetInEBO);
        } else {
            glDrawArrays(primitiveType, vertexOffset, vertexCount);
        }
        if (Debug.debugMesh) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }

        return true;
    }

    @Override
    public void close() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
