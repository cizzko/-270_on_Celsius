package core.g2d;

import core.pool.Pool;
import core.util.Disposable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Objects;

import static core.g2d.StackfulRender.defaultShader;
import static core.g2d.Render.*;
import static core.util.Color.toGLBits;
import static java.lang.Math.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;

public final class RenderQueue implements Disposable {
    private static final Logger log = LogManager.getLogger();

    public static final int VERTEX_PER_ITEM     = 4;
    public static final int VERTEX_PER_TRIANGLE = 6;

    public static final boolean USE_INDEXES = true;
    private static int MAX_ITEMS;

    public int getItemIndex() {
        return buffer[bufferIndex].items.size();
    }

    static final class Frame {
        final int size;
        final ObjectArrayList<RenderItem> items;
        final FloatBuffer vertices;
        final Mesh mesh;

        Frame(int size, int vertexCount) {
            this.size     = size;
            this.items    = new ObjectArrayList<>(new RenderItem[size], true) { };

            // 6 на прямоугольник в случае без индексов, с ними 4
            var vertexFormat = defaultShader.vertexFormat();
            this.vertices = MemoryUtil.memAllocFloat(vertexCount * vertexFormat.vertexSizeIn(Float.BYTES));
            this.mesh     = new Mesh();

            mesh.bindVao();
            vertexFormat.enableAttributes();
            mesh.vboUpload(vertices);
        }

        boolean hasSpace(int itemCount, int vertexCount) {
            return items.size() + itemCount < size &&
                   vertices.remaining() >= vertexCount * defaultShader.vertexFormat().vertexSizeIn(Float.BYTES);
        }

        public void close() {
            mesh.close();
            MemoryUtil.memFree(vertices);
            items.clear();
        }

        void debug() {
            if (log.isDebugEnabled()) {
                log.debug("HasSpace : {}", hasSpace(1, 1));
                log.debug("Items    : {}/{}", items.size(), size);
                log.debug("Vertices : {}/{}", vertices.position(), vertices.capacity());
            }
        }

        public boolean isEmpty() {
            return items.isEmpty() && vertices.position() == 0;
        }
    }

    final Pool<RenderItem> allocator;
    final @Nullable ElementBufferObject ebo;

    private final int primitiveType = GL_TRIANGLES;
    private final Frame[] buffer;
    private int bufferIndex;

    public RenderQueue(int itemCount, int vertexCount, int bufferInProcess) {
        this.allocator = new Pool<>(RenderItem::new, itemCount * bufferInProcess);
        this.buffer = new Frame[bufferInProcess];

        vertexCount = max(USE_INDEXES ? 4 : 6, vertexCount);
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new Frame(itemCount, vertexCount);
        }

        if (!USE_INDEXES) {
            ebo  = null;
        } else {
            int quadCount = vertexCount * VERTEX_PER_ITEM;
            var indices  = MemoryUtil.memAllocInt(quadCount * VERTEX_PER_TRIANGLE);
            for (int i = 0; i < quadCount; i++) {
                int baseVertex = i * VERTEX_PER_ITEM;
                indices.put(baseVertex);
                indices.put(baseVertex + 1);
                indices.put(baseVertex + 2);
                indices.put(baseVertex + 2);
                indices.put(baseVertex + 3);
                indices.put(baseVertex);
            }

            indices.flip();
            ebo = new  ElementBufferObject(indices);

            buffer[bufferIndex].mesh.bindVao();
            ebo.bind();
            ebo.upload(GL_STATIC_DRAW);
        }
    }

    public RenderItem allocItem() {
        return allocator.obtain();
    }

    public void push(RenderItem item) {
        frame(1, 0).items.add(item);
    }

    public int getVertexCountPerQuad() {
        return RenderQueue.USE_INDEXES ? 4 : 6;
    }

    public void advice(int itemCount, int vertexCount) {
        Frame frame = buffer[bufferIndex];
        if (!frame.hasSpace(itemCount, vertexCount)) {
            submitCommandList(frame);
        }
    }

    private Frame frame(int itemCount, int vertexCount) {
        Frame frame = buffer[bufferIndex];
        if (!frame.hasSpace(itemCount, vertexCount)) {
            submitCommandList(frame);
            frame = buffer[nextFrame()];
        }
        return frame;
    }

    private int nextFrame() {
        return bufferIndex = (bufferIndex + 1) % buffer.length;
    }

    public void beginFrame() {

    }

    /// Возвращает смещение внутри массива ВЕРШИН, но НЕ в FloatBuffer
    public int addRectangle(int rgba8888,
                            float x, float y, // левый нижний
                            float x2, float y2,
                            float x3, float y3,
                            float x4, float y4,
                            float u, float v,
                            float u2, float v2) {
        if (primitiveType == GL_TRIANGLE_STRIP) {
            float minX = min(min(x, x2), min(x3, x4));
            float maxX = max(max(x, x2), max(x3, x4));
            float minY = min(min(y, y2), min(y3, y4));
            float maxY = max(max(y, y2), max(y3, y4));

            float color = toGLBits(rgba8888);
            var frame = frame(0, 4);
            var va = frame.vertices;
            int offset = va.position() / defaultShader.vertexFormat().vertexSizeIn(Float.BYTES);

            addVertex0(va, minX, maxY,    u,  v,   color);  // TL
            addVertex0(va, maxX, maxY,    u2, v,   color);  // TR
            addVertex0(va, minX, minY, u,  v2,  color);  // BL
            addVertex0(va, maxX, minY, u2, v2,  color);  // BR
            return offset;
        }

        return addRectangle(
                x, y, x2, y2, x3, y3, x4, y4,
                u, v,
                u2, v2,
                toGLBits(rgba8888));
    }

    public int addRectangle(int rgba8888,
                            float x, float y,
                            float x2, float y2,
                            float u, float v,
                            float u2, float v2) {

        if (primitiveType == GL_TRIANGLE_STRIP) {
            float color = toGLBits(rgba8888);
            var frame = frame(0, 4);
            var va = frame.vertices;
            int offset = va.position() / defaultShader.vertexFormat().vertexSizeIn(Float.BYTES);
            addVertex0(va, x, y2,    u,  v,   color);
            addVertex0(va, x2, y2,    u2, v,   color);
            addVertex0(va, x, y, u,  v2,  color);
            addVertex0(va, x2, y, u2, v2,  color);
            return offset;

        }

        return addRectangle(
                x, y, x, y2, x2, y2, x2, y,
                u, v,
                u2, v2,
                toGLBits(rgba8888));
    }

    public int addRectangle(float x1, float y1,
                            float x2, float y2,
                            float x3, float y3,
                            float x4, float y4,
                            float u1, float v1,
                            float u2, float v2,
                            float c) {
        if (primitiveType == GL_TRIANGLE_STRIP) {
            var frame = frame(0, 4);
            var va = frame.vertices;
            int offset = va.position() / defaultShader.vertexFormat().vertexSizeIn(Float.BYTES);
            addVertex0(va, x1, y1, u1,  v1,  c);  // TL
            addVertex0(va, x2, y2, u2, v1,   c);  // TR
            addVertex0(va, x3, y3, u1,  v2,  c);  // BL
            addVertex0(va, x4, y4, u2, v2,   c);  // BR
            return offset;
        }

        if (!USE_INDEXES) {
            var frame = frame(0, 6);
            var va = frame.vertices;
            int offset = va.position() / defaultShader.vertexFormat().vertexSizeIn(Float.BYTES);
            addVertex0(va, x1, y1, u1, v2, c); // v1
            addVertex0(va, x2, y2, u1, v1, c); // v2
            addVertex0(va, x3, y3, u2, v1, c); // v3
            addVertex0(va, x1, y1, u1, v2, c); // v1
            addVertex0(va, x3, y3, u2, v1, c); // v3
            addVertex0(va, x4, y4, u2, v2, c); // v4
            return offset;
        } else {
            var frame = frame(0, 4);
            var va = frame.vertices;
            int offset = va.position() / defaultShader.vertexFormat().vertexSizeIn(Float.BYTES);
            addVertex0(va, x1, y1, u1, v2, c);
            addVertex0(va, x2, y2, u1, v1, c);
            addVertex0(va, x3, y3, u2, v1, c);
            addVertex0(va, x4, y4, u2, v2, c);
            return offset;
        }
    }

    public int addVertex(float x, float y, float u, float v, float color) {
        var frame = frame(0, 1);
        var va = frame.vertices;
        int offset = va.position() / defaultShader.vertexFormat().vertexSizeIn(Float.BYTES);
        addVertex0(va, x, y, u, v, color);
        return offset;
    }

    private void addVertex0(FloatBuffer va, float x, float y, float u, float v, float color) {
        va.put(x);
        va.put(y);
        va.put(color);
        va.put(u);
        va.put(v);
    }

    public void endFrame() {
        submitCommandList(buffer[bufferIndex]);
        nextFrame();
    }

    private void submitCommandList(Frame frame) {
        if (frame.isEmpty()) {
            return;
        }

        var items = frame.items;

        MAX_ITEMS = max(MAX_ITEMS, items.size());

        Arrays.parallelSort(items.elements(), 0, items.size(), RenderItem.Comparator.INSTANCE);

        // log.debug("BUFFER[{}]", bufferIndex);
        // frame.debug();

        var vertices = frame.vertices;
        var mesh = frame.mesh;
        vertices.flip();

        int currentLayer = -1;
        int currentTextureId = -1;
        int currentShaderId = -1;
        int currentBlending = -1;

        int groupIndexOffset = 0;
        int groupVertexOffset = 0;

        int groupIndexCount = 0;
        int groupVertexCount = 0;

        VertexFormat currentVertexFormat = defaultShader.vertexFormat();

        for (int i = 0; i < items.size(); ++i) {
            var item = items.get(i);

            byte layer = getLayer(item.sortKey);
            byte blending = getBlending(item.sortKey);
            short textureId = getTextureId(item.sortKey);
            byte shaderId = getShaderId(item.sortKey);

            boolean sameGroup = (
                    layer == currentLayer &&
                    blending == currentBlending &&
                    textureId == currentTextureId &&
                    shaderId == currentShaderId) &&
                    // разрыв, придётся отдельным вызовом сделать
                    (groupIndexOffset + item.vertexCount == item.vertexOffset);

            if (sameGroup) {
                groupIndexCount  += item.indexCount;
                groupVertexCount += item.vertexCount;
            } else {

                mesh.draw(primitiveType,
                        vertices, groupVertexOffset, groupVertexCount,
                        ebo, groupIndexOffset, groupIndexCount,
                        currentVertexFormat);

                if (currentBlending != blending) {
                    switch (blending) {
                        case BLENDING_NORMAL -> {
                            glEnable(GL_BLEND);
                            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                        }
                        default -> throw new IllegalStateException("Unknown blending " + blending);
                    }
                }

                var shader = ShaderCache.shadersById.get(shaderId);
                Objects.requireNonNull(shader, ShaderCache.shadersById::toString);
                if (currentShaderId != shaderId) {
                    shader.use();
                }

                mesh.bindVao();
                shader.vertexFormat().enableAttributes();
                currentVertexFormat = shader.vertexFormat();

                if (currentTextureId != textureId)
                    shader.setUniform("u_texture", textureId, 0);

                shader.setUniformTransforming("u_proj", item.matrix);

                currentLayer = layer;
                currentBlending = blending;
                currentTextureId = textureId;
                currentShaderId = shaderId;

                groupVertexOffset = item.vertexOffset;
                groupIndexOffset = item.indexOffset;

                groupIndexCount = item.indexCount;
                groupVertexCount = item.vertexCount;
            }
        }

        mesh.draw(primitiveType,
                vertices, groupVertexOffset, groupVertexCount,
                ebo, groupIndexOffset, groupIndexCount,
                currentVertexFormat);

        items.forEach(allocator::free);
        items.clear();
        vertices.clear();
    }

    @Override
    public void close() {
        for (Frame frame1 : buffer) {
            frame1.close();
        }
        if (ebo != null) ebo.close();
        allocator.clear();
    }
}
