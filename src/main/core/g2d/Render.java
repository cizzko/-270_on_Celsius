package core.g2d;

import org.intellij.lang.annotations.MagicConstant;

import static org.lwjgl.opengl.GL11.*;

public final class Render {

    private Render() {}

    // Всего: 1 << 2
    public static final byte PRIMITIVE_TYPE_TRIANGLES      = 0; // GL_TRIANGLES
    public static final byte PRIMITIVE_TYPE_TRIANGLE_STRIP = 1; // GL_TRIANGLE_STRIP
    public static final byte PRIMITIVE_TYPE_LINES          = 2; // GL_LINES
    public static final byte PRIMITIVE_TYPE_LINE_STRIP     = 3; // GL_LINE_STRIP

    // Всего: 1 << 3
    public static final byte LAYER_BACKGROUND = 0;
    public static final byte LAYER_BLOCKS     = 1;
    public static final byte LAYER_ENTITIES   = 2;
    public static final byte LAYER_GUI        = 3;
    public static final byte LAYER_DEBUG      = 4;

    // Всего: 1 << 3
    public static final byte BLENDING_NORMAL  = 0;

    // SortKey:
    // [ 63..62 | 61..59  | 58..56  | 55..40    | 39..32   | 31..24 |  23..0   ]
    //   ^ prim   ^ layer   ^ blend   ^ texture   ^ shader   ^ ublock  ^ index
    //   (2 bit)  (3 bit)   (3 bit)   (16 bit)    (8 bit)    (8 bit)   (24 bit)

    private static final byte PRIMITIVE_TYPE_SHIFT = 62;
    private static final byte LAYER_SHIFT   = 59;
    private static final byte BLEND_SHIFT   = 56;
    private static final byte TEXTURE_SHIFT = 40;
    private static final byte SHADER_SHIFT  = 32;
    private static final byte UBLOCK_SHIFT  = 24;

    private static final byte  PRIMITIVE_TYPE_MASK = (byte) 0b11; // 2 бита
    private static final byte  LAYER_MASK = (byte) 0x7;           // 3 бита
    private static final byte  BLEND_MASK = (byte) 0x7;           // 3 бита
    private static final short TEXTURE_MASK = (short) 0xFFFF;     // 16 битов
    private static final byte  SHADER_MASK = (byte) 0xFF;         // 8 битов
    private static final int   UBLOCK_MASK = (byte) 0xFF;         // 8 битов
    private static final int   INDEX_MASK = 0xFFFFFF;             // 24 бита

    public static int toGlType(@MagicConstant(intValues = {PRIMITIVE_TYPE_TRIANGLES, PRIMITIVE_TYPE_TRIANGLE_STRIP, PRIMITIVE_TYPE_LINES, PRIMITIVE_TYPE_LINE_STRIP}) int primitiveType) {
        return switch (primitiveType) {
            case -1 -> -1; // забавно
            case PRIMITIVE_TYPE_TRIANGLES -> GL_TRIANGLES;
            case PRIMITIVE_TYPE_TRIANGLE_STRIP -> GL_TRIANGLE_STRIP;
            case PRIMITIVE_TYPE_LINES -> GL_LINES;
            case PRIMITIVE_TYPE_LINE_STRIP -> GL_LINE_STRIP;
            default -> throw new IllegalArgumentException("Invalid primitive type: " + primitiveType);
        };
    }

    @MagicConstant(intValues = {PRIMITIVE_TYPE_TRIANGLES, PRIMITIVE_TYPE_TRIANGLE_STRIP, PRIMITIVE_TYPE_LINES, PRIMITIVE_TYPE_LINE_STRIP})
    public static byte getPrimitiveType(long sortKey) {
        return (byte) ((sortKey >>> PRIMITIVE_TYPE_SHIFT) & PRIMITIVE_TYPE_MASK);
    }

    @MagicConstant(intValues = {LAYER_BLOCKS})
    public static byte getLayer(long sortKey) {
        return (byte) ((sortKey >>> LAYER_SHIFT) & LAYER_MASK);
    }

    @MagicConstant(intValues = {BLENDING_NORMAL})
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

    public static long makeSortKey(
            byte primitiveType,
            byte layer,
            byte blending,
            short texture,
            byte shader,
            int ublock,
            int index) {

        return (long)(primitiveType & PRIMITIVE_TYPE_MASK) << PRIMITIVE_TYPE_SHIFT |
               (long)(layer & LAYER_MASK)       << LAYER_SHIFT |
               (long)(blending & BLEND_MASK)    << BLEND_SHIFT |
               (long)(texture & TEXTURE_MASK)   << TEXTURE_SHIFT |
               (long)(shader & SHADER_MASK)     << SHADER_SHIFT |
               (long)(ublock & UBLOCK_MASK)     << UBLOCK_SHIFT |
               (index & INDEX_MASK);
    }

    /// Максимальное количество вершин в [RenderList]
    static final int RENDER_MAX_VERTEX_COUNT  = 64 * 1024;
    /// Максимальное количество [RenderItem] в [RenderList]
    static final int RENDER_MAX_ITEMS_COUNT   = 32 * 1024;

    public static final RenderQueue queue = new RenderQueue(
            RENDER_MAX_ITEMS_COUNT, RENDER_MAX_VERTEX_COUNT);

    public static void init() {
        StackfulRender.state().rlist = queue.allocRList(RenderList.KIND_DYNAMIC);
        StackfulRender.state().shader = StackfulRender.defaultShader;
    }

    public static RenderQueue queue() { return queue; }
    public static RenderItem allocItem() { return queue.allocItem(); }
}
