package core.g2d;

import core.pool.Poolable;
import core.util.Disposable;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static core.g2d.Render.*;
import static core.g2d.RenderItem.*;
import static core.g2d.RenderQueue.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;

public final class RenderList implements Poolable, Disposable {
    public static final byte KIND_STATIC  = 1;
    public static final byte KIND_DYNAMIC = 2;

    private static final ValueLayout.OfInt PRIMITIVE = ValueLayout.JAVA_INT;

    static final class Vertex {
        static final StructLayout LAYOUT;
        static final long BYTE_SIZE;

        static final VarHandle X;
        static final VarHandle Y;
        static final VarHandle UV;
        static final VarHandle COLOR;

        static {
            var layout = MemoryLayout.structLayout(
                    ValueLayout.JAVA_FLOAT.withName("x"),
                    ValueLayout.JAVA_FLOAT.withName("y"),
                    PRIMITIVE.withName("color"),
                    PRIMITIVE.withName("uv")
            ).withByteAlignment(4);

            X = field(layout, "x");
            Y = field(layout, "y");
            UV = field(layout, "uv");
            COLOR = field(layout, "color");

            LAYOUT = layout;
            BYTE_SIZE = layout.byteSize();
        }

        static VarHandle field(StructLayout layout, String name) {
            var field = layout.arrayElementVarHandle(groupElement(name)).withInvokeExactBehavior();
            return MethodHandles.insertCoordinates(field, 1, 0L).withInvokeExactBehavior();
        }
    }

    final byte id;
    byte kind;

    @Nullable RenderList next, prev;

    final int itemCapacity, vertexCapacity;

    // Количество монотонных кусков в массиве. Изначально считаем, что кусок один.
    int runCount = 1;
    // Направление текущего куска: 1 = растем, -1 = падаем, 0 = только начали
    int currentDirection = 0;

    int itemCount, vertexCount;

    final long[] sortKeys;
    final MemorySegment items;
    final MemorySegment vertices;
    final Mesh mesh;

    final UniformBuffer uniforms = new UniformBuffer();

    public UniformBuffer uniformBuffer() { return uniforms; }

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
               ", items=" + itemCount + "/" + itemCapacity +
               ", vertices=" + vertices +
               '}';
    }

    RenderList(byte id, Arena rarena, int itemCapacity, int vertexCapacity) {
        this.id = id;
        this.vertexCapacity = vertexCapacity;
        this.itemCapacity = itemCapacity;
        this.sortKeys = new long[itemCapacity];
        this.items = rarena.allocate(RenderItem.LAYOUT, itemCapacity);

        this.vertices = rarena.allocate(Vertex.LAYOUT, vertexCapacity);
        this.mesh     = new Mesh();

        mesh.vboUpload(vertices);
    }

    public int getItemIndex()   { return itemCount; }
    public int getVertexIndex() { return vertexCount; }

    public boolean hasSpace(int itemDelta, int vertexDelta) {
        return this.itemCount + itemDelta < itemCapacity &&
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
        runCount = 1;
        currentDirection = 0;
        uniforms.clear();
        mesh.reset();
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
        long vc = vertexCount;
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

    private void addVertex0(MemorySegment va, long vertexOffset,
                            float x, float y, float u, float v, int color) {

        Vertex.X.set(va,     vertexOffset, x);
        Vertex.Y.set(va,     vertexOffset, y);
        Vertex.COLOR.set(va, vertexOffset, Integer.reverseBytes(color));
        Vertex.UV.set(va,    vertexOffset, BytePack.packB16toInt32((short) u, (short) v));
    }

    public void addVertex(float x, float y, float u, float v, int color) {
        addVertex0(vertices, vertexCount, x,  y, u, v, color);
        vertexCount++;
    }

    public void push(long sortKey,
                     int vertexOffset, short vertexCount,
                     int indexOffset,  short indexCount) {

        int idx = itemCount;
        sortKeys[idx] = sortKey;

        if (idx > 0) {
            long prevKey = sortKeys[idx - 1];
            int cmp = Long.compareUnsigned(sortKey, prevKey);

            assert cmp != 0; // Инвариант sortKey

            int newDirection = cmp > 0 ? 1 : -1;
            if (currentDirection == 0) {
                currentDirection = newDirection;
            } else if (newDirection != currentDirection) {
                runCount++;
                currentDirection = newDirection;
            }
        }

        var items = this.items;
        long offset = idx;
        VERTEX_OFFSET.set(items, offset, vertexOffset);
        INDEX_OFFSET.set(items,  offset, indexOffset);
        VERTEX_COUNT.set(items,  offset, vertexCount);
        INDEX_COUNT.set(items,   offset, indexCount);

        itemCount++;
    }

    public void debug() {
        if (log.isDebugEnabled()) {
            log.debug("RenderList[{}]", id);
            log.debug("Items        : {}/{}", itemCount, itemCapacity);
            log.debug("Vertices     : {}/{}", vertexCount, vertexCapacity);
            log.debug("RunCount     : {}", runCount);
            log.debug("RunDirection : {}", currentDirection);
        }
    }

    @Override
    public void reset() {
        clear();
        for (var it = next; it != null; it = it.next) {
            it.clear();
        }
    }

    @Override
    public void close() {
        mesh.close();
        // RenderQueue отслеживает что насоздавала,
        // так что сама отчистит связанный список
        // Связи намерено не зануляю
    }
}
