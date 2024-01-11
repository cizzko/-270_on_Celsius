package core.g2d;

import java.util.List;

public final class VertexFormat {
    private final List<VertexAttribute> vertexAttributes;
    private final int[] offsets;
    private final int vertexByteSize;

    public VertexFormat(List<VertexAttribute> vertexAttributes) {
        this.vertexAttributes = List.copyOf(vertexAttributes);
        this.offsets = new int[vertexAttributes.size()];

        int vsize = 0;
        for (int i = 0; i < vertexAttributes.size(); i++) {
            VertexAttribute attr = vertexAttributes.get(i);
            offsets[i] = vsize;
            vsize += attr.byteSize();
        }
        this.vertexByteSize = vsize;
    }

    public static VertexFormat of(List<VertexAttribute> vertexAttributes) {
        return new VertexFormat(vertexAttributes);
    }

    public int vertexByteSize() {
        return vertexByteSize;
    }

    public void enableAttributes() {
        for (int i = 0; i < vertexAttributes.size(); i++) {
            VertexAttribute attr = vertexAttributes.get(i);
            attr.enable(i, vertexByteSize, offsets[i]);
        }
    }

    public void disableAttributes() {
        for (int i = 0; i < vertexAttributes.size(); i++) {
            VertexAttribute attr = vertexAttributes.get(i);
            attr.disable(i);
        }
    }
}
