package core.g2d;

import core.pool.Pool;
import core.util.Disposable;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;

import static core.g2d.RenderList.*;
import static core.g2d.StackfulRender.defaultShader;
import static core.g2d.Render.*;
import static java.lang.Math.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;

public final class RenderQueue implements Disposable {
    static final Logger log = LogManager.getLogger();

    public static final int VERTEX_PER_ITEM     = 4;
    public static final int VERTEX_PER_TRIANGLE = 6;

    public static final boolean USE_INDEXES = true;
    private static int MAX_ITEMS;

    final Pool<RenderItem> ritemAlloc;
    final Pool<RenderList> rlistAlloc;
    final @Nullable ElementBufferObject ebo;

    private static final int primitiveType = GL_TRIANGLES;

    private int rlistCount;

    private final ArrayDeque<RenderList> renderLists = new ArrayDeque<>();
    private final ObjectArrayList<RenderList> created = new ObjectArrayList<>();

    public RenderQueue(int itemCount, int vertexCount) {
        this.ritemAlloc = new Pool<>(RenderItem::new, itemCount);
        this.rlistAlloc = new Pool<>(() -> {
            var list = new RenderList(rlistCount++, itemCount, vertexCount);
            created.add(list);
            return list;
        }, 10);


        if (!USE_INDEXES) {
            ebo  = null;
        } else {
            int capedVertCount = max(USE_INDEXES ? 4 : 6, vertexCount);
            int quadCount = capedVertCount * VERTEX_PER_ITEM;
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
            ebo.bind();
            ebo.upload(GL_STATIC_DRAW);
        }
    }

    public RenderItem allocItem() { return ritemAlloc.obtain(); }

    public int getVertexCountPerQuad() { return RenderQueue.USE_INDEXES ? 4 : 6; }

    public void beginFrame() {
    }

    public void endFrame() {
        RenderList it;
        while ((it = renderLists.pollFirst()) != null)
            submitCommandList(it);
    }

    public RenderList allocRList(@MagicConstant(intValues = {KIND_STATIC, KIND_DYNAMIC}) int kind) {
        var rlist = switch (kind) {
            case KIND_STATIC -> rlistAlloc.create();
            case KIND_DYNAMIC -> rlistAlloc.obtain();
            default -> throw new IllegalArgumentException("Invalid kind: " + kind);
        };
        rlist.kind = kind;
        return rlist;
    }

    public void push(RenderList renderList) {
        if (renderLists.contains(renderList)) {
            throw new IllegalStateException(renderList.id + " already in queue");
        }
        renderLists.addLast(renderList);
    }

    public void submitCommandList(RenderList rlist) {
        for (var it = rlist; it != null; it = it.next) {
            submitRenderList(it);
        }
    }

    private void submitRenderList(RenderList rlist) {
        log.debug("RList[{}]", rlist.id);
        if (rlist.isEmpty()) {
            return;
        }

        var items = rlist.items;

        MAX_ITEMS = max(MAX_ITEMS, items.size());

        Arrays.parallelSort(items.elements(), 0, items.size(), RenderItem.Comparator.INSTANCE);

        var vertices = rlist.vertices;
        var mesh = rlist.mesh;

        int vapos = vertices.position(), vacount = vertices.limit();
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

                if (currentBlending != blending)
                    setBlending(blending);

                var shader = ShaderCache.shadersById.get(shaderId);
                Objects.requireNonNull(shader, ShaderCache.shadersById::toString);
                if (currentShaderId != shaderId)
                    shader.use();

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

        switch (rlist.kind) {
            // Обратно восставливаем write-mode
            case RenderList.KIND_STATIC -> {
                vertices.position(vapos);
                vertices.limit(vacount);
            }
            case RenderList.KIND_DYNAMIC -> rlistAlloc.free(rlist);
        }
    }

    private static void setBlending(byte blending) {
        switch (blending) {
            case BLENDING_NORMAL -> {
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }
            default -> throw new IllegalStateException("Unknown blending " + blending);
        }
    }

    @Override
    public void close() {
        if (ebo != null) ebo.close();
        ritemAlloc.clear();
        rlistAlloc.clear();
        for (RenderList renderList : created) {
            renderList.close();
        }
    }
}
