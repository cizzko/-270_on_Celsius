package core.g2d;

import core.Global;

import java.lang.foreign.ValueLayout;
import java.util.Arrays;

import static core.g2d.OpenGL.DSA;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL46C.*;

public final class VertexFormat {
    private final VertexAttribute[] attributes;
    private final int[] offsets;
    private final int byteSize;

    int vao;
    int refCount = 1;

    public VertexFormat(VertexAttribute... attributes) {
        this.attributes = attributes;
        this.offsets = new int[attributes.length];

        int vsize = 0;
        for (int i = 0; i < attributes.length; i++) {
            VertexAttribute attr = attributes[i];
            offsets[i] = vsize;
            vsize += attr.byteSize();
        }
        this.byteSize = vsize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VertexFormat that)) return false;
        return byteSize == that.byteSize
               && Arrays.equals(offsets, that.offsets)
               && Arrays.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + h + Arrays.hashCode(attributes);
        h += (h << 5) + h + Arrays.hashCode(offsets);
        h += (h << 5) + h + byteSize;
        h += (h << 5) + h + vao;
        return h;
    }

    @Override
    public String toString() {
        return "VertexFormat{vao=" + vao + ", " +
               "attributes=" + Arrays.toString(attributes) +
               ", byteSize=" + byteSize +
               '}';
    }

    public int vertexByteSize() { return byteSize; }
    public int vertexSizeIn(ValueLayout unit) { return byteSize / (int)unit.byteSize(); }

    void enableAttributes() {
        if (DSA) {
            for (int i = 0; i < attributes.length; i++) {
                attributes[i].enable(vao, i, byteSize, offsets[i]);
            }
        } else { // требует заблаговременного бинда vao
            for (int i = 0; i < attributes.length; i++) {
                attributes[i].enable(i, byteSize, offsets[i]);
            }
        }
    }

    void bindVBO(int vbo) {
        if (DSA) {
            glVertexArrayVertexBuffer(vao, 0, vbo, 0, byteSize);
        } else { // требует заблаговременного бинда vao
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
        }
    }

    void bindEBO(int ebo) {
        if (DSA) {
            glVertexArrayElementBuffer(vao, ebo);
        } else { // требует заблаговременного бинда vao
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        }
    }

    void disableAttributes() {
        for (int i = 0; i < attributes.length; i++) {
            attributes[i].disable(i);
        }
    }

    int getVAO() {
        int vaoId = vao;
        if (vaoId == 0) {
            Global.renderThread.ensureThisThread();
            vao = vaoId = OpenGL.createVertexArrays();

            OpenGL.bindVertexArray(vaoId);
            enableAttributes();
            if (Render.queue.ebo != null) {
                bindEBO(Render.queue.ebo.id);
            }
            // знаю свой код и поэтому не делаю это
            // OpenGL.bindVertexArray(0);
        }
        return vaoId;
    }
}
