package core.g2d;

import core.util.Debug;
import core.util.Disposable;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.MemorySegment;

import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL46.*;

public final class Mesh implements Disposable {

    int vbo;

    boolean dirty = true;
    boolean atLeastOneBind = true;
    boolean vertexFormatChanged = true;
    VertexFormat vertexFormat;

    public Mesh() {
        vbo = OpenGL.createBuffer();
    }

    public void setDirty(boolean state) { dirty = state; }

    public void setup(VertexFormat format, @Nullable ElementBufferObject ebo) {
        if (atLeastOneBind) {
            atLeastOneBind = false;
        } else {
            if (!vertexFormatChanged) {
                return;
            }
            vertexFormatChanged = false;
        }

        glBindVertexArray(format.vao);

        if (OpenGL.DSA) {
            format.bindVBO(vbo);
        } else {
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            format.enableAttributes();
            // if (ebo != null) ebo.bind();
        }
    }

    public void vboUpload(MemorySegment buffer) { // не забудь vao
        OpenGL.setBufferData(vbo, GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
    }

    public void setVertexFormat(VertexFormat vertexFormat) {
        if (vertexFormat != this.vertexFormat) {
            this.vertexFormat = vertexFormat;
            vertexFormatChanged = true;
        } else {
            vertexFormatChanged = false;
        }
    }

    public boolean draw(int primitiveType,
                        MemorySegment vertices, int vertexOffset, int vertexCount,
                        @Nullable ElementBufferObject ebo, int indexOffset, int indexCount) {

        if (vertexCount == 0) {
            return false;
        }

        var format = vertexFormat;
        setup(format, ebo);

        if (dirty) {
            int stride = format.vertexByteSize();
            long byteOffset = (long) vertexOffset * stride;
            long byteSize = (long) vertexCount * stride;
            OpenGL.bufferSubData(vbo, GL_ARRAY_BUFFER, byteOffset, byteSize, vertices);
        }

        if (Debug.debugMesh) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }
        if (ebo != null) {
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

    public void reset() {
        atLeastOneBind = true;
        vertexFormatChanged = true;
        vertexFormat = null;
    }

    @Override
    public void close() {
        glDeleteBuffers(vbo);
    }
}
