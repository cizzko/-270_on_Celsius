package core.util;

public class FixedBitset {

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
        if (idx >= 0 && idx < bits.length) {
            return (bits[idx] & (1L << i)) != 0;
        }
        throw new IndexOutOfBoundsException();
    }
}
