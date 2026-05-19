package core.util;

import java.util.Objects;

public class FixedBitset {


    public static int pos2index(int sizeX, int x, int y) { return x + sizeX * y; }
    public static int index2x(int sizeX, int index)      { return index % sizeX; }
    public static int index2y(int sizeX, int index)      { return index / sizeX; }

    public static long[] createBitSet(int n) {
        return new long[((n - 1) >> 6) + 1];
    }

    public static void setBit(long[] bits, int i) {
        int idx = i >> 6;
        if (idx >= 0 && idx < bits.length) {
            bits[idx] |= 1L << i;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public static void unsetBit(long[] bits, int i) {
        int idx = i >> 6;
        if (idx >= 0 && idx < bits.length) {
            bits[idx] &= ~(1L << i);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public static boolean isSet(long[] bits, int i) {
        int idx = i >> 6;
        Objects.checkIndex(idx, bits.length);
        return (bits[idx] & (1L << i)) != 0;
    }
}
