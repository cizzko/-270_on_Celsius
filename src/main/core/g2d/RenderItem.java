package core.g2d;

import com.fasterxml.jackson.annotation.JsonValue;
import core.pool.Poolable;

import java.util.Arrays;

public final class RenderItem implements Poolable {
    public /* unsigned */ long sortKey;
    public final float[] matrix = new float[9];
    public int vertexOffset, vertexCount;
    public int indexOffset, indexCount;

    public void validate() {
        assert vertexOffset >= 0;
        assert vertexCount > 0;
        assert indexOffset >= 0;
        assert indexCount > 0;
    }

    @Override
    public void reset() {
        sortKey = 0;
        vertexOffset = vertexCount = 0;
        indexOffset = indexCount = 0;
        Arrays.fill(matrix, 0);
    }

    @JsonValue
    @Override
    public String toString() {
        return "RenderItem{" +
               "sortKey=" + Long.toUnsignedString(sortKey, 16) +
               ", layer=" + Render.getLayer(sortKey) +
               ", blending=" + Render.getBlending(sortKey) +
               ", textureId=" + Render.getTextureId(sortKey) +
               ", shaderId=" + Render.getShaderId(sortKey) +
               ", index=" + Render.getIndex(sortKey) +
               ", vertexOffset=" + vertexOffset +
               ", vertexCount=" + vertexCount +
               '}';
    }

    public enum Comparator implements java.util.Comparator<RenderItem> {
        INSTANCE;

        @Override
        public int compare(RenderItem o1, RenderItem o2) { return Long.compareUnsigned(o1.sortKey, o2.sortKey); }
    }
}
