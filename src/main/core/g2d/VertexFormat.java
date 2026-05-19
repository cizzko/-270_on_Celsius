package core.g2d;

import java.util.Arrays;

public final class VertexFormat {
    private final VertexAttribute[] attributes;
    private final int[] offsets;
    private final int byteSize;

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
    public String toString() {
        return "VertexFormat{" +
               "attributes=" + Arrays.toString(attributes) +
               ", byteSize=" + byteSize +
               '}';
    }

    public int vertexByteSize() { return byteSize; }
    public int vertexSizeIn(int unit) { return byteSize / unit; }

    public void enableAttributes() {
        for (int i = 0; i < attributes.length; i++) {
            attributes[i].enable(i, byteSize, offsets[i]);
        }
    }

    public void disableAttributes() {
        for (int i = 0; i < attributes.length; i++) {
            attributes[i].disable(i);
        }
    }
}
