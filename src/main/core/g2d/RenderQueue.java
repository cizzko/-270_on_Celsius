package core.g2d;

import core.gen.Uniforms;
import core.pool.Pool;
import core.util.Disposable;
import core.util.TimSort;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.lang.foreign.Arena;
import java.util.ArrayDeque;

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

    final Pool<RenderList> rlistAlloc;
    final @Nullable ElementBufferObject ebo;

    private int rlistCount;

    private final ArrayDeque<RenderList> renderLists = new ArrayDeque<>();
    private final ObjectArrayList<RenderList> created = new ObjectArrayList<>();
    private final UniformBuffer uniformBuffer = new UniformBuffer();

    private final RenderItem[] tmp;
    private final TimSort<RenderItem> sorter = new TimSort<>();
    private final Arena renderArena = Arena.ofConfined();

    public RenderQueue(int itemCount, int vertexCount) {
        this.tmp = new RenderItem[itemCount];
        this.rlistAlloc = new Pool<>(() -> {
            var list = new RenderList(rlistCount++, renderArena, itemCount, vertexCount);
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
            ebo = new ElementBufferObject(indices);
            ebo.upload(GL_STATIC_DRAW);
        }
    }

    public UniformBuffer uniformBuffer() { return uniformBuffer; }

    public short getVertexCountPerQuad(@PrimitiveType byte primitiveType) {
        return switch (primitiveType) {
            case PRIMITIVE_TYPE_TRIANGLE_STRIP -> 4;
            case PRIMITIVE_TYPE_TRIANGLES -> USE_INDEXES ? 4 : 6;
            default -> throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType);
        };
    }

    public void beginFrame() {
        // Здесь могла быть ваша статистика
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
        sorter.sort(items, tmp, RenderItem.Comparator.INSTANCE, 0, rlist.itemCount);

        var vertices = rlist.vertices;
        var mesh = rlist.mesh;

        int currentPrimitiveType = -1;
        int currentBlending = -1;
        int currentTextureId = -1;
        int currentShaderId = -1;
        int currentUblock = -1;

        int groupIndexOffset = 0;
        int groupVertexOffset = 0;

        int groupIndexCount = 0;
        int groupVertexCount = 0;

        mesh.setVertexFormat(defaultShader.vertexFormat());

        long prevSortKey = 0;

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = rlist.itemCount; i < n; ++i) {
            var item = items[i];

            boolean sameGroup =
                    (prevSortKey & EXCLUDE_INDEX_MASK) == (item.sortKey & EXCLUDE_INDEX_MASK) &&
                    // разрыв, придётся отдельным вызовом сделать
                    (groupVertexOffset + groupVertexCount == item.vertexOffset);

            if (sameGroup) {
                groupIndexCount  += item.indexCount;
                groupVertexCount += item.vertexCount;
            } else {
                byte primitiveType = getPrimitiveType(item.sortKey);
                byte blending = getBlending(item.sortKey);
                short textureId = getTextureId(item.sortKey);
                byte shaderId = getShaderId(item.sortKey);
                byte ublock = getUblock(item.sortKey);

                mesh.draw(currentPrimitiveType,
                        vertices, groupVertexOffset, groupVertexCount,
                        ebo, groupIndexOffset, groupIndexCount);

                if (currentBlending != blending) {
                    setBlending(blending);
                }

                var shader = ResourceCache.shadersById[shaderId];

                if (currentShaderId != shaderId) {
                    shader.use();
                }

                mesh.setVertexFormat(shader.vertexFormat);

                if (currentUblock != ublock) {
                    var block = uniformBuffer.id2blocks[ublock];
                    block.use(shaderId);
                }

                if (OpenGL.GL_ARB_bindless_texture || currentTextureId != textureId) {
                    OpenGL.bindTexture(shaderId, Uniforms.DefaultShader.u_texture, textureId, 0);
                }

                currentPrimitiveType = toGlType(primitiveType);
                currentBlending = blending;
                currentTextureId = textureId;
                currentShaderId = shaderId;
                currentUblock = ublock;

                prevSortKey = item.sortKey;

                groupVertexOffset = item.vertexOffset;
                groupIndexOffset = item.indexOffset;

                groupIndexCount = item.indexCount;
                groupVertexCount = item.vertexCount;
            }
        }

        mesh.draw(currentPrimitiveType,
                vertices, groupVertexOffset, groupVertexCount,
                ebo, groupIndexOffset, groupIndexCount);

        if (rlist.kind == KIND_DYNAMIC) {
            rlistAlloc.freeAndReset(rlist);
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
        for (RenderList renderList : created) {
            renderList.close();
        }
        renderArena.close();
    }
}
