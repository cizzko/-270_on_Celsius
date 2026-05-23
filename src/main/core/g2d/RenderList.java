package core.g2d;

import core.pool.Poolable;
import core.util.Disposable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static core.g2d.Render.*;
import static core.g2d.RenderQueue.*;
import static core.g2d.StackfulRender.defaultShader;
import static core.graphic.Color.toGLBits;
import static java.lang.Math.max;
import static java.lang.Math.min;

public final class RenderList implements Disposable, Poolable {
    public static final int KIND_STATIC  = 1;
    public static final int KIND_DYNAMIC = 2;

    final int id;

    int kind;
    @Nullable RenderList next, prev;

    int prevItemIdx, prevVertexIdx;

    final int itemCapacity, vertexCapacity;
    final ObjectArrayList<RenderItem> items;
    final FloatBuffer vertices;
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
               "prevItemIdx=" + prevItemIdx +
               ", prevVertexIdx=" + prevVertexIdx +
               ", items=" + items.size() + "/" + itemCapacity +
               ", vertices=" + vertices +
               '}';
    }

    RenderList(int id, int itemCount, int vertexCount) {
        this.id = id;
        this.itemCapacity = itemCount;
        this.vertexCapacity = vertexCount;
        this.items    = new ObjectArrayList<>(new RenderItem[itemCount], true) { };

        // 6 на прямоугольник в случае без индексов, с ними 4
        var vertexFormat = defaultShader.vertexFormat();
        this.vertices = MemoryUtil.memAllocFloat(vertexCount * vertexFormat.vertexSizeIn(Float.BYTES));
        this.mesh     = new Mesh();

        mesh.bindVao();
        vertexFormat.enableAttributes();
        mesh.vboUpload(vertices);
    }

    public int getItemIndex()   { return items.size(); }
    public int getVertexIndex() { return vertices.position() / defaultShader.vertexFormat().vertexSizeIn(Float.BYTES); }

    public boolean hasSpace(int itemCount, int vertexCount) {
        return items.size() + itemCount < itemCapacity &&
               vertices.remaining() >= vertexCount * defaultShader.vertexFormat().vertexSizeIn(Float.BYTES);
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
            return n;
        }
        return this;
    }

    public boolean isEmpty() {
        return items.isEmpty() && vertices.position() == 0;
    }

    public void begin() {
        prevItemIdx   = items.size();
        prevVertexIdx = vertices.position();
    }

    public boolean end() {
        return prevItemIdx == items.size() && prevVertexIdx == vertices.position();
    }

    public void clear() {
        items.forEach(queue.ritemAlloc::free);
        items.clear();
        vertices.clear();
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
            addRectangle(primitiveType, x, y, x2, y2, x3, y3, x4, y4, u, v, u2, v2, toGLBits(rgba8888));
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

        float color = toGLBits(rgba8888);
        var va = vertices;
        addVertex0(va, minX, maxY, u, v,   color);  // TL
        addVertex0(va, maxX, maxY, u2, v,   color);  // TR
        addVertex0(va, minX, minY, u, v2,  color);  // BL
        addVertex0(va, maxX, minY, u2, v2,  color);  // BR
    }

    public void addRectangle(int primitiveType, int rgba8888,
                            float x1, float y1,
                            float x2, float y2,
                            float u1, float v1,
                            float u2, float v2) {

        if (primitiveType == PRIMITIVE_TYPE_TRIANGLE_STRIP) {
            float color = toGLBits(rgba8888);
            var va = vertices;

            addVertex0(va, x1, y1, u1, v1, color); // BL
            addVertex0(va, x1, y2, u1, v2, color); // TL
            addVertex0(va, x2, y2, u2, v2, color); // TR
            addVertex0(va, x2, y1, u2, v1, color); // BR
        } else {
            addRectangle(primitiveType,
                    x1, y1, x1, y2, x2, y2, x2, y1,
                    u1, v1,
                    u2, v2,
                    toGLBits(rgba8888));
        }
    }

    public void addRectangle(int primitiveType, float x1, float y1,
                            float x2, float y2,
                            float x3, float y3,
                            float x4, float y4,
                            float u1, float v1,
                            float u2, float v2,
                            float c) {
        var va = vertices;
        if (primitiveType == PRIMITIVE_TYPE_TRIANGLE_STRIP) {
            addVertex0(va, x1, y1, u1,  v1,  c);  // TL
            addVertex0(va, x2, y2, u2, v1,   c);  // TR
            addVertex0(va, x3, y3, u1,  v2,  c);  // BL
            addVertex0(va, x4, y4, u2, v2,   c);  // BR
            return;
        }

        if (!USE_INDEXES) {
            addVertex0(va, x1, y1, u1, v2, c); // v1
            addVertex0(va, x2, y2, u1, v1, c); // v2
            addVertex0(va, x3, y3, u2, v1, c); // v3
            addVertex0(va, x1, y1, u1, v2, c); // v1
            addVertex0(va, x3, y3, u2, v1, c); // v3
            addVertex0(va, x4, y4, u2, v2, c); // v4
        } else {
            addVertex0(va, x1, y1, u1, v2, c);
            addVertex0(va, x2, y2, u1, v1, c);
            addVertex0(va, x3, y3, u2, v1, c);
            addVertex0(va, x4, y4, u2, v2, c);
        }
    }

    private void addVertex0(FloatBuffer va, float x, float y, float u, float v, float color) {
        va.put(x);
        va.put(y);
        va.put(color);
        va.put(BytePack.packB16toFloat32((short) u, (short) v));
    }

    public void addVertex(float x, float y, float u, float v, float color) {
        addVertex0(vertices, x,  y, u, v, color);
    }

    public void push(RenderItem item) {
        items.add(item);
    }

    @Override
    public void close() {
        MemoryUtil.memFree(vertices);
        items.clear();
    }

    void debug() {
        if (log.isDebugEnabled()) {
            log.debug("HasSpace : {}", hasSpace(1, 1));
            log.debug("Items    : {}/{}", items.size(), itemCapacity);
            log.debug("Vertices : {}/{}", vertices.position(), vertices.capacity());
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
