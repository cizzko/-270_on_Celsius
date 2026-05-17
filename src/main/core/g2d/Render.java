package core.g2d;

import org.intellij.lang.annotations.MagicConstant;

public final class Render {
    private Render() {}

    // Всего: 1 << 3
    public static final byte LAYER_BACKGROUND = 0;
    public static final byte LAYER_BLOCKS     = 1;
    public static final byte LAYER_ENTITIES   = 2;
    public static final byte LAYER_GUI        = 3;
    public static final byte LAYER_DEBUG      = 4;

    // Всего: 1 << 3
    public static final byte BLENDING_NORMAL  = 0;

    // SortKey:
    // [61..59  | 58..56  | 55..40    | 39..32   | 31..0 ]
    //  ^ layer   ^ blend   ^ texture   ^ shader   ^ index
    //  (3 bit)   (3 bit)   (16 bit)    (8 bit)    (32 bit)

    private static final byte LAYER_SHIFT = 59;
    private static final byte BLEND_SHIFT = 56;
    private static final byte TEXTURE_SHIFT = 40;
    private static final byte SHADER_SHIFT = 32;

    // Маски для очистки данных (чтобы не вышли за пределы битов)
    private static final byte  LAYER_MASK = (byte) 0x7;       // 3 бита
    private static final byte  BLEND_MASK = (byte) 0x7;       // 3 бита
    private static final short TEXTURE_MASK = (byte) 0xFFFF;    // 16 битов
    private static final byte  SHADER_MASK = (byte) 0xFF;       // 8 битов
    private static final long INDEX_MASK = 0xFFFFFFFFL;  // 32 бита

    @MagicConstant(intValues = {LAYER_BLOCKS})
    public static byte getLayer(long sortKey) {
        return (byte) ((sortKey >>> LAYER_SHIFT) & LAYER_MASK);
    }

    @MagicConstant(intValues = {Render.BLENDING_NORMAL})
    public static byte getBlending(long sortKey) {
        return (byte) ((sortKey >>> BLEND_SHIFT) & BLEND_MASK);
    }

    public static short getTextureId(long sortKey) {
        return (short) ((sortKey >>> TEXTURE_SHIFT) & TEXTURE_MASK);
    }

    public static /* unsigned */ byte getShaderId(long sortKey) {
        return (byte) ((sortKey >>> SHADER_SHIFT) & SHADER_MASK);
    }

    public static int getIndex(long sortKey) {
        return (int) (sortKey & INDEX_MASK);
    }

    public static long makeSortKey(
            byte layer,
            byte blending,
            short texture,
            byte shader,
            int index) {

        return (long)(layer & LAYER_MASK)       << LAYER_SHIFT |
               (long)(blending & BLEND_MASK)    << BLEND_SHIFT |
               (long)(texture & TEXTURE_MASK)   << TEXTURE_SHIFT |
               (long)(shader & SHADER_MASK)     << SHADER_SHIFT |
               (index & INDEX_MASK);
    }

    static final int RENDER_MAX_ITEMS_COUNT   = 32 * 1024;
    static final int RENDER_MAX_VERTEX_COUNT  = 64 * 1024;
    static final int RENDER_BUFFER_IN_PROCESS = 2;

    public static final RenderQueue queue = new RenderQueue(
            RENDER_MAX_ITEMS_COUNT, RENDER_MAX_VERTEX_COUNT,
            RENDER_BUFFER_IN_PROCESS);
    public static RenderQueue queue() { return queue; }
    public static RenderItem allocItem() { return queue.allocItem(); }
}
