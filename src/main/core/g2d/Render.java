package core.g2d;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.intellij.lang.annotations.MagicConstant;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;

public final class Render {

    private Render() {}

    @Retention(RetentionPolicy.SOURCE)
    @MagicConstant(intValues = {PRIMITIVE_TYPE_TRIANGLES, PRIMITIVE_TYPE_TRIANGLE_STRIP, PRIMITIVE_TYPE_LINES, PRIMITIVE_TYPE_LINE_STRIP})
    public @interface PrimitiveType {}

    @Retention(RetentionPolicy.SOURCE)
    @MagicConstant(intValues = {LAYER_BACKGROUND, LAYER_BLOCKS, LAYER_ENTITIES, LAYER_GUI, LAYER_DEBUG})
    public @interface Layer {}

    @Retention(RetentionPolicy.SOURCE)
    @MagicConstant(intValues = {BLENDING_NORMAL, BLENDING_PREMUL, BLENDING_DISABLE})
    public @interface Blending {}

    // region PrimitiveType
    public static final byte PRIMITIVE_TYPE_TRIANGLES      = 0; // GL_TRIANGLES
    public static final byte PRIMITIVE_TYPE_TRIANGLE_STRIP = 1; // GL_TRIANGLE_STRIP
    public static final byte PRIMITIVE_TYPE_LINES          = 2; // GL_LINES
    public static final byte PRIMITIVE_TYPE_LINE_STRIP     = 3; // GL_LINE_STRIP
    // endregion

    // region Layer
    public static final byte LAYER_BACKGROUND = 0;
    public static final byte LAYER_BLOCKS     = 1;
    public static final byte LAYER_ENTITIES   = 2;
    public static final byte LAYER_GUI        = 3;
    public static final byte LAYER_DEBUG      = 4;
    // endregion

    // region Blending
    public static final byte BLENDING_NORMAL  = 0;
    public static final byte BLENDING_PREMUL  = 1;
    public static final byte BLENDING_DISABLE = 2;
    // endregion

    // Технические ограничения рендера
    // Значения здесь выбраны исходя из потребностей и реального использования
    public static final int  MAX_PRIMITIVE_TYPE = 1 << 2;
    public static final int  MAX_LAYER          = 1 << 3;
    public static final int  MAX_BLEND          = 1 << 3;
    public static final int  MAX_TEXTURE        = 1 << 16;
    // На самом деле
    public static final int  MAX_SHADER         = 1 << 8;
    public static final int  MAX_UBLOCK         = 1 << 8;
    public static final int  MAX_INDEX          = 1 << 16;

    public static final char MAX_TEXTURE_ID     = MAX_TEXTURE - 1;
    public static final byte MAX_SHADER_ID      = (byte)(MAX_SHADER - 1);
    public static final byte MAX_UBLOCK_ID      = (byte)(MAX_UBLOCK - 1);

    // SortKey:
    // [ 63 | 55..54  | 53..51  | 50..48  | 47..32    | 31..24   | 23..16   | 15..0   ]
    //        ^ prim    ^ layer   ^ blend   ^ texture   ^ shader   ^ ublock   ^ index
    //        (2 bit)   (3 bit)   (3 bit)   (16 bit)    (8 bit)    (8 bit)    (16 bit)

    // 63 бит зарезервирован для нужд Самарской Области
    // А если по-простому: мегаоптимизированный Arrays.sort(long[]) который подходит для большинства случаев
    // не хочет принимать кастомный компаратор, поэтому там обычное сравнение знаковых long,
    // что феерически не будет работать с sortKey

    private static final byte PRIMITIVE_TYPE_SHIFT = 54;
    private static final byte LAYER_SHIFT          = 51;
    private static final byte BLEND_SHIFT          = 48;
    private static final byte TEXTURE_SHIFT        = 32;
    private static final byte SHADER_SHIFT         = 24;
    private static final byte UBLOCK_SHIFT         = 16;

    static final long  PRIMITIVE_TYPE_MASK          = MAX_PRIMITIVE_TYPE - 1;
    static final long  LAYER_MASK                   = MAX_LAYER - 1;
    static final long  BLEND_MASK                   = MAX_BLEND - 1;
    static final long  TEXTURE_MASK                 = MAX_TEXTURE - 1;
    static final long  SHADER_MASK                  = MAX_SHADER - 1;
    static final long  UBLOCK_MASK                  = MAX_UBLOCK - 1;
    static final long  INDEX_MASK                   = MAX_INDEX - 1;

    static final long EXCLUDE_INDEX_MASK            = ~((long) INDEX_MASK);

    public static int toGlType(@PrimitiveType int primitiveType) {
        return switch (primitiveType) {
            case -1 -> -1; // забавно
            case PRIMITIVE_TYPE_TRIANGLES -> GL_TRIANGLES;
            case PRIMITIVE_TYPE_TRIANGLE_STRIP -> GL_TRIANGLE_STRIP;
            case PRIMITIVE_TYPE_LINES -> GL_LINES;
            case PRIMITIVE_TYPE_LINE_STRIP -> GL_LINE_STRIP;
            default -> throw new IllegalArgumentException("Invalid primitive type: " + primitiveType);
        };
    }

    @SuppressWarnings("MagicConstant")
    @PrimitiveType
    public static byte getPrimitiveType(long sortKey) {
        return (byte) ((sortKey >>> PRIMITIVE_TYPE_SHIFT) & PRIMITIVE_TYPE_MASK);
    }

    @SuppressWarnings("MagicConstant")
    @Layer
    public static byte getLayer(long sortKey) {
        return (byte) ((sortKey >>> LAYER_SHIFT) & LAYER_MASK);
    }

    @SuppressWarnings("MagicConstant")
    @Blending
    public static byte getBlending(long sortKey) {
        return (byte) ((sortKey >>> BLEND_SHIFT) & BLEND_MASK);
    }

    public static short getTextureId(long sortKey) {
        return (short) ((sortKey >>> TEXTURE_SHIFT) & TEXTURE_MASK);
    }

    public static /* unsigned */ byte getShaderId(long sortKey) {
        return (byte) ((sortKey >>> SHADER_SHIFT) & SHADER_MASK);
    }

    public static byte getUblock(long sortKey) {
        return (byte) ((sortKey >>> UBLOCK_SHIFT) & UBLOCK_MASK);
    }

    public static int getIndex(long sortKey) {
        return (int) (sortKey & INDEX_MASK);
    }

    public static String sortKeyToString(long sortKey) {
        return "SortKey[" +
               "primitiveType=" + getPrimitiveType(sortKey) +
               ", layer=" + getLayer(sortKey) +
               ", blending=" + getBlending(sortKey) +
               ", texture=" + getTextureId(sortKey) +
               ", shader=" + getShaderId(sortKey) +
               ", ublock=" + getUblock(sortKey) +
               ", index=" + getIndex(sortKey) +
               "]";
    }

    public static long makeSortKey(
            @PrimitiveType byte primitiveType,
            @Layer byte layer,
            @Blending byte blending,
            short texture,
            byte shader,
            int ublock,
            int index) {

        return (primitiveType & PRIMITIVE_TYPE_MASK) << PRIMITIVE_TYPE_SHIFT |
               (layer & LAYER_MASK) << LAYER_SHIFT |
               (blending & BLEND_MASK) << BLEND_SHIFT |
               (texture & TEXTURE_MASK) << TEXTURE_SHIFT |
               (shader & SHADER_MASK) << SHADER_SHIFT |
               (ublock & UBLOCK_MASK) << UBLOCK_SHIFT |
               (index & INDEX_MASK);
    }

    public static final int VERTEX_PER_ITEM     = 4;
    public static final int VERTEX_PER_TRIANGLE = 6;

    public static final boolean USE_INDEXES = true;

    /// Максимальное количество вершин в [RenderList]
    static final int RENDER_MAX_VERTEX_COUNT  = 4 * 4 * 1024;
    /// Максимальное количество [RenderItem] в [RenderList]
    static final int RENDER_MAX_ITEMS_COUNT   = 4 * 1024;

    /// До сколько забегов отрезков с нарушенным порядком в [RenderList]
    /// будет использоваться JDK [java.util.Arrays#sort(long\[\], int, int)]
    static final int JDK_SORT_MIN_RUNS = 9;

    public static final RenderQueue queue = new RenderQueue(
            RENDER_MAX_ITEMS_COUNT, RENDER_MAX_VERTEX_COUNT);

    public static void init() {
        StackfulRender.state().rlist = queue.allocRList(RenderList.KIND_DYNAMIC);
        StackfulRender.state().shader = StackfulRender.defaultShader;
    }

    private static final ObjectOpenHashSet<VertexFormat> cache = new ObjectOpenHashSet<>();

    public static void dispose() {
        for (var vertexFormat : cache) {
            glDeleteVertexArrays(vertexFormat.vao);
        }
        cache.clear();
    }

    public static RenderQueue queue() { return queue; }

    public static void dispose(VertexFormat format) {
        if (cache.remove(format)) {
            if (--format.refCount == 0) {
                glDeleteVertexArrays(format.vao);
            }
        }
    }

    public static VertexFormat setupVAO(VertexFormat vertexFormat) {
        var ex = cache.addOrGet(vertexFormat);
        if (ex != null && vertexFormat != ex) {
            ex.refCount++;
            return ex;
        }
        vertexFormat.vao = OpenGL.createVertexArrays();
        vertexFormat.enableAttributes();
        if (queue.ebo != null) {
            vertexFormat.bindEBO(queue.ebo.id);
        }
        return vertexFormat;
    }
}
