package core.g2d;

import core.Global;
import core.gen.Uniforms;
import core.pool.Pool;
import core.util.Disposable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import java.lang.foreign.Arena;
import java.util.ArrayDeque;
import java.util.Arrays;

import static core.g2d.Render.*;
import static core.g2d.RenderItem.*;
import static core.g2d.RenderList.KIND_DYNAMIC;
import static core.g2d.RenderList.KIND_STATIC;
import static core.g2d.StackfulRender.defaultShader;
import static java.lang.Math.max;
import static org.lwjgl.opengl.GL46C.*;

public final class RenderQueue implements Disposable {
    static final Logger log = LogManager.getLogger();

    static final byte MAX_ID = Byte.MAX_VALUE;

    final Pool<RenderList> rlistAlloc;
    final @Nullable ElementBufferObject ebo;

    private byte rlistCount;

    private final ArrayDeque<RenderList> renderLists = new ArrayDeque<>();
    private final ObjectArrayList<RenderList> created = new ObjectArrayList<>();

    private final Arena renderArena = Arena.ofShared();

    private final long[] tmp;

    byte genId() {
        byte i = rlistCount;
        if (i == MAX_ID) {
            throw new IllegalStateException("Limit of render lists exceeded");
        }
        rlistCount++;
        return i;
    }

    public RenderQueue(int itemCount, int vertexCount) {
        this.rlistAlloc = new Pool<>(() -> {
            var list = new RenderList(genId(), renderArena, itemCount, vertexCount);
            created.add(list);
            return list;
        }, 10);

        tmp = new long[itemCount];

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

        var array = new RenderList[3] ;
        for (int i = 0; i < array.length; i++) {
            array[i] = allocRList(KIND_DYNAMIC);
        }
        buffer = new TripleBuffer(array);
    }

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

    public RenderList allocRList(@MagicConstant(intValues = {KIND_STATIC, KIND_DYNAMIC}) byte kind) {
        var rlist = switch (kind) {
            case KIND_STATIC -> rlistAlloc.create();
            case KIND_DYNAMIC -> rlistAlloc.obtain();
            default -> throw new IllegalArgumentException("Invalid kind: " + kind);
        };
        rlist.kind = kind;
        return rlist;
    }

    public final TripleBuffer buffer;

    public void push(RenderList renderList) {
        if (renderList.isEmpty()) {
            return;
        }
        renderLists.addLast(renderList);
    }

    public void submitCommandList(RenderList rlist) {
        for (var it = rlist; it != null; it = it.next) {
            submitRenderList(it);
        }
    }

    private void submitRenderList(RenderList rlist) {
        if (rlist.isEmpty()) {
            return;
        }

        var sortKeys = rlist.sortKeys;
        var items = rlist.items;
        var vertices = rlist.vertices;
        var mesh = rlist.mesh;
        int itemCount = rlist.itemCount;

        // Инварианты для компилятора
        if (itemCount >= sortKeys.length) return;
        if (itemCount * RenderItem.BYTE_SIZE >= items.byteSize()) return;

        int runCount = rlist.runCount;
        int c;
        long t;
        // Предполагает что количество нарушений порядка меньше чем itemCount/16
        // поскольку реализация векторизованная на адекватных машинах
        if (runCount < JDK_SORT_MIN_RUNS || runCount < (itemCount >>> 4)) {
            c = 0;
            t = System.nanoTime();
            Arrays.sort(sortKeys, 0, itemCount);
            t = System.nanoTime() - t;
        } else {
            c = 1;
            t = System.nanoTime();
            RadixSort.sort(sortKeys, tmp, itemCount);
            t = System.nanoTime() - t;
        }

        if (Global.input.justPressed(GLFW.GLFW_KEY_F4)) {
            rlist.debug();
            log.debug("TimeNS: {}", t);
            log.debug("Type: {}", c == 0 ? "JDK" : "Radix");
        }

        var ebo = this.ebo;

        long prevSortKey = 0;

        int currentPrimitiveType = -1; // тип в GL
        int currentBlending     = -1;
        int currentTextureId   = -1;
        int currentShaderId     = -1;
        int currentUblock       = -1;

        int groupIndexOffset  = 0;
        int groupVertexOffset = 0;

        int groupIndexCount   = 0;
        int groupVertexCount  = 0;


        mesh.setVertexFormat(defaultShader.vertexFormat());

        //noinspection ForLoopReplaceableByForEach
        for (int ai = 0; ai < itemCount; ++ai) {
            long sortKey = sortKeys[ai];

            long offset = getIndex(sortKey);

            int vertexOffset  = (int)VERTEX_OFFSET.get(items,  offset);
            int indexOffset   = (int)INDEX_OFFSET.get(items,   offset);
            short vertexCount = (short)VERTEX_COUNT.get(items, offset);
            short indexCount  = (short)INDEX_COUNT.get(items,  offset);

            boolean sameGroup =
                    (prevSortKey & EXCLUDE_INDEX_MASK) == (sortKey & EXCLUDE_INDEX_MASK) &&
                    // разрыв, придётся отдельным вызовом сделать
                    (groupVertexOffset + groupVertexCount == vertexOffset);
                    // TODO сверять и для индексов, но сейчас не критично

            if (sameGroup) {
                groupIndexCount  += indexCount;
                groupVertexCount += vertexCount;
            } else {
                byte  primitiveType = getPrimitiveType(sortKey);
                byte  blending = getBlending(sortKey);
                short textureId = getTextureId(sortKey);
                byte  shaderId = getShaderId(sortKey);
                byte  ublock = getUblock(sortKey);

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
                    var block = rlist.uniforms.id2blocks[ublock];
                    block.use(shader);
                }

                if (OpenGL.GL_ARB_bindless_texture || currentTextureId != textureId) {
                    OpenGL.bindTexture(shaderId, Uniforms.DefaultShader.u_texture, textureId, 0);
                }

                currentPrimitiveType = toGlType(primitiveType);
                currentBlending = blending;
                currentTextureId = textureId;
                currentShaderId = shaderId;
                currentUblock = ublock;

                prevSortKey = sortKey;

                groupVertexOffset = vertexOffset;
                groupIndexOffset = indexOffset;

                groupIndexCount = indexCount;
                groupVertexCount = vertexCount;
            }
        }

        mesh.draw(currentPrimitiveType,
                vertices, groupVertexOffset, groupVertexCount,
                ebo, groupIndexOffset, groupIndexCount);
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
        for (var rlist : created) {
            rlist.close();
        }
        renderArena.close();
    }
}
