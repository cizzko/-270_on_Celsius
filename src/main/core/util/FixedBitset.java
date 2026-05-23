package core.util;

import java.util.Objects;

// Адаптированный java.util.BitSet
public class FixedBitset {

    private static final long WORD_MASK = 0xffffffffffffffffL;

    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;

    private static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    public static int pos2index(int sizeX, int x, int y) { return x + sizeX * y; }
    public static int index2x(int sizeX, int index)      { return index % sizeX; }
    public static int index2y(int sizeX, int index)      { return index / sizeX; }

    public static long[] createBitSet(int n) {
        return new long[((n - 1) >> ADDRESS_BITS_PER_WORD) + 1];
    }

    public static void setRange(long[] bitset, int fromIndex, int toIndex) {
        Objects.checkFromToIndex(fromIndex, toIndex, bitset.length * BITS_PER_WORD);
        if (fromIndex == toIndex)
            return;

        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex   = wordIndex(toIndex - 1);

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask  = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            bitset[startWordIndex] |= (firstWordMask & lastWordMask);
        } else {
            bitset[startWordIndex] |= firstWordMask;

            for (int i = startWordIndex+1; i < endWordIndex; i++)
                bitset[i] = WORD_MASK;

            bitset[endWordIndex] |= lastWordMask;
        }
    }

    public static void setBit(long[] bits, int i) {
        int idx = wordIndex(i);
        Objects.checkIndex(idx, bits.length);
        bits[idx] |= 1L << i;
    }

    public static void unsetBit(long[] bits, int i) {
        int idx = wordIndex(i);
        Objects.checkIndex(idx, bits.length);
        bits[idx] &= ~(1L << i);
    }

    public static boolean isSet(long[] bits, int i) {
        int idx = wordIndex(i);
        Objects.checkIndex(idx, bits.length);
        return (bits[idx] & (1L << i)) != 0;
    }
}
