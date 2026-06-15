package core.g2d;

import org.lwjgl.opengl.GL46;

import java.lang.foreign.ValueLayout;
import java.util.Arrays;

import static core.g2d.OpenGL.DSA;

public final class VertexFormat {
    private final VertexAttribute[] attributes;
    private final int[] offsets;
    private final int byteSize;

    int vao;
    int refCount;

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

    public static VertexFormat of(VertexAttribute... vertexAttributes) {
        return new VertexFormat(vertexAttributes);
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

    public void enableAttributes() {
        if (DSA) {
            for (int i = 0; i < attributes.length; i++) {
                attributes[i].enable(vao, i, byteSize, offsets[i]);
            }
        } else {
            for (int i = 0; i < attributes.length; i++) {
                attributes[i].enable(i, byteSize, offsets[i]);
            }
        }
    }

    public void bindVBO(int vbo) {
        if (!DSA) {
            return;
        }
        GL46.glVertexArrayVertexBuffer(vao, 0, vbo, 0, byteSize);
    }

    public void bindEBO(int ebo) {
        if (DSA) {
            GL46.glVertexArrayElementBuffer(vao, ebo);
        } else {
            GL46.glBindVertexArray(vao);
            GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, ebo);

            GL46.glBindVertexArray(0); // происходит редко, но метко
        }
    }

    public void disableAttributes() {
        for (int i = 0; i < attributes.length; i++) {
            attributes[i].disable(i);
        }
    }
}
