package core.g2d.r2;

import core.g2d.Render;
import org.intellij.lang.annotations.MagicConstant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SortKeyTests {// Всего: 1 << 3
    public static final byte LAYER_BACKGROUND = 0;
    public static final byte LAYER_BLOCKS = 1;
    public static final byte LAYER_ENTITIES = 2;
    public static final byte LAYER_GUI = 3;
    public static final byte LAYER_DEBUG = 4;

    // Всего: 1 << 3
    public static final byte BLENDING_NORMAL = 0;

    // SortKey:
    // [61..59  | 58..56  | 55..40    | 39..32   | 31..0 ]
    //  ^ layer   ^ blend   ^ texture   ^ shader   ^ index
    //  (3 bit)   (3 bit)   (16 bit)    (8 bit)    (32 bit)

    private static final byte LAYER_SHIFT = 59;
    private static final byte BLEND_SHIFT = 56;
    private static final byte TEXTURE_SHIFT = 40;
    private static final byte SHADER_SHIFT = 32;

    // Маски для очистки данных (чтобы не вышли за пределы битов)
    private static final byte LAYER_MASK = (byte) 0x7;       // 3 бита
    private static final byte BLEND_MASK = (byte) 0x7;       // 3 бита
    private static final short TEXTURE_MASK = (byte) 0xFFFF;    // 16 битов
    private static final byte SHADER_MASK = (byte) 0xFF;       // 8 битов
    private static final long INDEX_MASK = 0xFFFFFFFFL;  // 32 бита

    public static int getLayer(long sortKey) {
        return (int) ((sortKey >>> LAYER_SHIFT) & LAYER_MASK);
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

    public static long makeSortKey(
            byte layer,
            byte blending,
            short texture,
            byte shader,
            int index) {

        return (long) (layer & LAYER_MASK) << LAYER_SHIFT |
               (long) (blending & BLEND_MASK) << BLEND_SHIFT |
               (long) (texture & TEXTURE_MASK) << TEXTURE_SHIFT |
               (long) (shader & SHADER_MASK) << SHADER_SHIFT |
               (index & INDEX_MASK);
    }

    @Test
    void test() {
        {
            long a = makeSortKey((byte) 0, (byte) 0, (short) 0, (byte) 0, 0);
            long b = makeSortKey((byte) 1, (byte) 0, (short) 0, (byte) 0, 1);
            long c = makeSortKey((byte) 1, (byte) 0, (short) 0, (byte) 1, 2);
            assertTrue(Long.compareUnsigned(a, b) < 0);
            assertTrue(Long.compareUnsigned(b, c) < 0);
            assertTrue(Long.compareUnsigned(a, c) < 0);
        }
        for (int l : new int[]{0, 1})
            for (int b : new int[]{0, 1})
                for (int t : new int[]{0, 1})
                    for (int s : new int[]{0, 1}) {
                        long a1 = makeSortKey((byte) l, (byte) b, (short) t, (byte) s, 0);
                        long a2 = makeSortKey((byte) l, (byte) b, (short) t, (byte) s, 1);
                        assertTrue(Long.compareUnsigned(a1, a2) < 0);
                    }
    }


}
