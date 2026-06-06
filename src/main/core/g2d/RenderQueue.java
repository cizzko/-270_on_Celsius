package core.g2d;

import core.pool.Pool;
import core.util.Disposable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;

import static core.g2d.Render.*;
import static core.g2d.RenderList.KIND_DYNAMIC;
import static core.g2d.RenderList.KIND_STATIC;
import static core.g2d.StackfulRender.defaultShader;
import static java.lang.Math.max;
import static org.lwjgl.opengl.GL46.*;

public final class RenderQueue implements Disposable {
    static final Logger log = LogManager.getLogger();

    public static final int VERTEX_PER_ITEM     = 4;
    public static final int VERTEX_PER_TRIANGLE = 6;

    public static final boolean USE_INDEXES = true;

    final Pool<RenderItem> ritemAlloc;
    final Pool<RenderList> rlistAlloc;
    final @Nullable ElementBufferObject ebo;

    private int rlistCount;

    private final ArrayDeque<RenderList> renderLists = new ArrayDeque<>();
    private final ObjectArrayList<RenderList> created = new ObjectArrayList<>();
    private final UniformBuffer uniformBuffer = new UniformBuffer();

    public RenderQueue(int itemCount, int vertexCount) {
        this.ritemAlloc = new Pool<>(RenderItem::new, itemCount);
        this.rlistAlloc = new Pool<>(() -> {
            var list = new RenderList(rlistCount++, itemCount, vertexCount);
            created.add(list);
            return list;
        }, 10);

        if (!USE_INDEXES) {
            ebo = null;
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

    public UniformBuffer uniformBuffer() { return uniformBuffer; }

    public RenderItem allocItem() { return ritemAlloc.obtain(); }

    public int getVertexCountPerQuad(@PrimitiveType byte primitiveType) {
        return switch (primitiveType) {
            case PRIMITIVE_TYPE_TRIANGLE_STRIP -> 4;
            case PRIMITIVE_TYPE_TRIANGLES -> USE_INDEXES ? 4 : 6;
            default -> throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType);
        };
    }

    public void beginFrame() {
    }

    public void endFrame() {
        if (renderLists.isEmpty()) {
            return;
        }

        drainCommandQueue();
        uniformBuffer.clear();
    }

    private void drainCommandQueue() {
        RenderList it;
        while ((it = renderLists.pollFirst()) != null) {
            submitCommandList(it);
        }
    }

    public void flush() {
        drainCommandQueue();
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
        if (renderList.isEmpty()) {
            return;
        }
        if (renderLists.contains(renderList)) {
            throw new IllegalStateException(renderList.id + " already in queue");
        }
        renderLists.addLast(renderList);
    }

    private void submitCommandList(RenderList rlist) {
        for (var it = rlist; it != null; it = it.next) {
            submitRenderList(it);
        }
    }

    private void submitRenderList(RenderList rlist) {
        if (rlist.isEmpty()) {
            return;
        }

        var items = rlist.items;

        Arrays.parallelSort(items.elements(), 0, items.size(), RenderItem.Comparator.INSTANCE);

        var vertices = rlist.vertices;
        var mesh = rlist.mesh;

        int vapos = vertices.position(), vacount = vertices.limit();
        vertices.flip();

        int currentPrimitiveType = -1;
        int currentLayer = -1;
        int currentBlending = -1;
        int currentTextureId = -1;
        int currentShaderId = -1;
        int currentUblock = -1;

        int groupIndexOffset = 0;
        int groupVertexOffset = 0;

        int groupIndexCount = 0;
        int groupVertexCount = 0;

        VertexFormat currentVertexFormat = defaultShader.vertexFormat();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < items.size(); ++i) {
            var item = items.get(i);

            byte primitiveType = getPrimitiveType(item.sortKey);
            byte layer = getLayer(item.sortKey);
            byte blending = getBlending(item.sortKey);
            short textureId = getTextureId(item.sortKey);
            byte shaderId = getShaderId(item.sortKey);
            byte ublock = getUblock(item.sortKey);

            boolean sameGroup = (
                    primitiveType == currentPrimitiveType &&
                    layer == currentLayer &&
                    blending == currentBlending &&
                    textureId == currentTextureId &&
                    shaderId == currentShaderId &&
                    ublock == currentUblock) &&
                    // разрыв, придётся отдельным вызовом сделать
                    (groupVertexOffset + groupVertexCount == item.vertexOffset);

            if (sameGroup) {
                groupIndexCount  += item.indexCount;
                groupVertexCount += item.vertexCount;
            } else {

                mesh.draw(toGlType(currentPrimitiveType),
                        vertices, groupVertexOffset, groupVertexCount,
                        ebo, groupIndexOffset, groupIndexCount,
                        currentVertexFormat);

                if (currentBlending != blending) {
                    setBlending(blending);
                }

                var shader = ResourceCache.shadersById.get(shaderId);
                Objects.requireNonNull(shader);

                if (currentShaderId != shaderId) {
                    shader.use();
                    mesh.bindVao();
                    shader.vertexFormat().enableAttributes();
                }

                if (currentUblock != ublock) {
                    var block = uniformBuffer.id2blocks.get(ublock);
                    block.setTo(shader);
                }

                if (currentTextureId != textureId) {
                    shader.setUniformTexture2d("u_texture", textureId, 0);
                }

                currentPrimitiveType = primitiveType;
                currentLayer = layer;
                currentBlending = blending;
                currentTextureId = textureId;
                currentShaderId = shaderId;
                currentUblock = ublock;

                currentVertexFormat = shader.vertexFormat();

                groupVertexOffset = item.vertexOffset;
                groupIndexOffset = item.indexOffset;

                groupIndexCount = item.indexCount;
                groupVertexCount = item.vertexCount;
            }
        }

        mesh.draw(toGlType(currentPrimitiveType),
                vertices, groupVertexOffset, groupVertexCount,
                ebo, groupIndexOffset, groupIndexCount,
                currentVertexFormat);

        switch (rlist.kind) {
            // восстанавливаем write-mode
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
            case BLENDING_PREMUL -> {
                glEnable(GL_BLEND);
                glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA,
                                    GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            }
            case BLENDING_DISABLE -> {
                glDisable(GL_BLEND);
            }
            default -> throw new IllegalStateException("Unknown blending " + blending);
        }
    }

    @Override
    public void close() {
        if (ebo != null) {
            ebo.close();
        }
        ritemAlloc.clear();
        rlistAlloc.clear();
        for (RenderList renderList : created) {
            renderList.close();
        }
    }
}
