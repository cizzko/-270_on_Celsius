package core.g2d;

import java.util.List;

public final class VertexFormat {
    private final VertexAttribute[] vertexAttributes;
    private final int[] offsets;
    private final int vertexByteSize;

    public VertexFormat(VertexAttribute... vertexAttributes) {
        this.vertexAttributes = vertexAttributes;
        this.offsets = new int[vertexAttributes.length];

        int vsize = 0;
        for (int i = 0; i < vertexAttributes.length; i++) {
            VertexAttribute attr = vertexAttributes[i];
            offsets[i] = vsize;
            vsize += attr.byteSize();
        }
        this.vertexByteSize = vsize;
    }

    public static VertexFormat of(VertexAttribute... vertexAttributes) {
        return new VertexFormat(vertexAttributes);
    }

    public int vertexByteSize() {
        return vertexByteSize;
    }

    public void enableAttributes() {
        for (int i = 0; i < vertexAttributes.length; i++) {
            vertexAttributes[i].enable(i, vertexByteSize, offsets[i]);
        }
    }

    public void disableAttributes() {
        for (int i = 0; i < vertexAttributes.length; i++) {
            vertexAttributes[i].disable(i);
        }
    }
}
