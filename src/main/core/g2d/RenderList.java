package core.g2d;

import core.pool.Poolable;
import core.util.Disposable;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static core.g2d.Render.*;
import static core.g2d.RenderQueue.*;
import static core.g2d.StackfulRender.defaultShader;
import static java.lang.Math.max;
import static java.lang.Math.min;

public final class RenderList implements Disposable, Poolable {
    public static final int KIND_STATIC  = 1;
    public static final int KIND_DYNAMIC = 2;

    private static final ValueLayout.OfInt PRIMITIVE = ValueLayout.JAVA_INT;

    final int id;

    int kind;
    @Nullable RenderList next, prev;

    final int vertexCapacity;
    final RenderItem[] items;
    int itemCount, vertexCount;
    final MemorySegment vertices;
    final Mesh mesh;

    public void setDirty(boolean state) {
        mesh.setDirty(state);
    }

    private static String kindToString(int kind) {
        return switch (kind) {
            case KIND_STATIC -> "STATIC";
            case KIND_DYNAMIC -> "DYNAMIC";
            default -> "UNKNOWN";
        };
    }

    @Override
    public String toString() {
        return "RenderList[" + id + ";" + kindToString(kind) + "]{" +
               (next != null ? ("next=" + next) : "") +
               (prev != null ? (", prev=" + prev + ", ") : "") +
               ", items=" + itemCount + "/" + items.length +
               ", vertices=" + vertices +
               '}';
    }

    RenderList(int id, Arena renderArena, int itemCapacity, int vertexCapacity) {
        this.id = id;
        this.vertexCapacity = vertexCapacity;
        this.items = new RenderItem[itemCapacity];
        for (int i = 0; i < items.length; i++) {
            items[i] = new RenderItem();
        }

        // 6 на прямоугольник в случае без индексов, с ними 4
        var vertexFormat = defaultShader.vertexFormat();
        this.vertices = renderArena.allocate(PRIMITIVE, Math.multiplyExact(vertexCapacity, vertexFormat.vertexSizeIn(PRIMITIVE)));
        this.mesh     = new Mesh();

        mesh.vboUpload(vertices);
    }

    public int getItemIndex()   { return itemCount; }
    public int getVertexIndex() { return vertexCount; }

    public RenderItem allocItem() { return items[itemCount]; }

    public boolean hasSpace(int itemDelta, int vertexDelta) {
        return this.itemCount + itemDelta < items.length &&
               this.vertexCount + vertexDelta < vertexCapacity;
    }

    public RenderList checkSpace(int itemCount, int vertexCount) {
        if (!hasSpace(itemCount, vertexCount)) {
            var n = next;
            var newRList = queue.allocRList(kind);
            if (n == null) {
                next = n = newRList;
            } else {
                newRList.prev = n;
                n.next = n = newRList;
            }
            assert false; // TODO планируют упростить
            return n;
        }
        return this;
    }

    public boolean isEmpty() {
        return itemCount == 0 && vertexCount == 0;
    }

    public void clear() {
        itemCount = 0;
        vertexCount = 0;
    }

    public void addRectangle(int primitiveType, int rgba8888,
                            float x, float y, // левый нижний
                            float x2, float y2,
                            float x3, float y3,
                            float x4, float y4,
                            float u, float v,
                            float u2, float v2) {
        if (primitiveType == PRIMITIVE_TYPE_TRIANGLE_STRIP) {
            addRectangleStripes(rgba8888, x, y, x2, y2, x3, y3, x4, y4, u, v, u2, v2);
        } else {
            addRectangle(x, y, x2, y2, x3, y3, x4, y4, u, v, u2, v2, (rgba8888));
        }
    }

    public void addRectangleStripes(int rgba8888,
                                    float x, float y,
                                    float x2, float y2,
                                    float x3, float y3,
                                    float x4, float y4,
                                    float u, float v,
                                    float u2, float v2) {
        float minX = min(min(x, x2), min(x3, x4));
        float maxX = max(max(x, x2), max(x3, x4));
        float minY = min(min(y, y2), min(y3, y4));
        float maxY = max(max(y, y2), max(y3, y4));

        var va = vertices;
        //addVertex0(va, minX, maxY, u, v, rgba8888);  // TL
        //addVertex0(va, maxX, maxY, u2, v, rgba8888);  // TR
        //addVertex0(va, minX, minY, u, v2, rgba8888);  // BL
        //addVertex0(va, maxX, minY, u2, v2, rgba8888);  // BR

        vertexCount += 4;
    }

    public void addRectangle(int rgba8888,
                             float x1, float y1,
                             float x2, float y2,
                             float u1, float v1,
                             float u2, float v2) {

        addRectangle(
                x1, y1, x1, y2,
                x2, y2, x2, y1,
                u1, v1, u2, v2,
                rgba8888);
    }

    public void addRectangle(float x1, float y1,
                             float x2, float y2,
                             float x3, float y3,
                             float x4, float y4,
                             float u1, float v1,
                             float u2, float v2,
                             int c) {
        int vc = vertexCount;
        var va = vertices;
        if (!USE_INDEXES) {
            addVertex0(va, vc + 0, x1, y1, u1, v2, c); // v1
            addVertex0(va, vc + 1, x2, y2, u1, v1, c); // v2
            addVertex0(va, vc + 2, x3, y3, u2, v1, c); // v3
            addVertex0(va, vc + 3, x1, y1, u1, v2, c); // v1
            addVertex0(va, vc + 4, x3, y3, u2, v1, c); // v3
            addVertex0(va, vc + 5, x4, y4, u2, v2, c); // v4

            vertexCount += 6;
        } else {
            addVertex0(va, vc + 0, x1, y1, u1, v2, c);
            addVertex0(va, vc + 1, x2, y2, u1, v1, c);
            addVertex0(va, vc + 2, x3, y3, u2, v1, c);
            addVertex0(va, vc + 3, x4, y4, u2, v2, c);

            vertexCount += 4;
        }
    }

    private void addVertex0(MemorySegment va, int vertexOffset,
                            float x, float y, float u, float v, int color) {
        long offset = vertexOffset * 4L;
        va.setAtIndex(ValueLayout.JAVA_FLOAT, offset + 0, x);
        va.setAtIndex(ValueLayout.JAVA_FLOAT, offset + 1, y);
        va.setAtIndex(PRIMITIVE, offset + 2, Integer.reverseBytes(color));
        va.setAtIndex(PRIMITIVE, offset + 3, BytePack.packB16toInt32((short) u, (short) v));
    }

    public void addVertex(float x, float y, float u, float v, int color) {
        addVertex0(vertices, vertexCount, x,  y, u, v, color);
        vertexCount++;
    }

    public void advance() {
        itemCount++;
    }

    public void push(RenderItem item) {
        items[itemCount++] = item;
    }

    @Override
    public void close() {
        // items это массив в куче, так что пусть сборщик сделает своё дело
        // Arrays.fill(items, null);
    }

    void debug() {
        if (log.isDebugEnabled()) {
            log.debug("HasSpace : {}", hasSpace(1, 1));
            log.debug("Items    : {}/{}", itemCount, items.length);
            log.debug("Vertices : {}/{}", vertexCount, vertexCapacity);
        }
    }

    @Override
    public void reset() {
        clear();
        for (var it = next; it != null; it = it.next) {
            it.clear();
        }
    }
}
