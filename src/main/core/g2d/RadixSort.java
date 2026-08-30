package core.g2d;

import java.util.Arrays;

public final class RadixSort {
    private RadixSort() {
    }


    private static final int BITS  = 64;
    private static final int SHIFT = 16; // index
    private static final int STEP  = 8;
    private static final int SIZE  = 1 << STEP;
    private static final int MASK  = SIZE - 1;

    private static final int[] radixCounts = new int[SIZE];

    public static void sort(long[] items, long[] tmp, int count) {
        if (count < 2) return;

        final int[] counts = radixCounts;

        pass(items, tmp, count, counts, SHIFT + STEP * 0);
        pass(tmp, items, count, counts, SHIFT + STEP * 1);
        pass(items, tmp, count, counts, SHIFT + STEP * 2);
        pass(tmp, items, count, counts, SHIFT + STEP * 3);
        pass(items, tmp, count, counts, SHIFT + STEP * 4);
        pass(tmp, items, count, counts, SHIFT + STEP * 5);
    }

    private static void pass(long[] src, long[] dest, int count, int[] counts, int shift) {
        Arrays.fill(counts, 0);

        for (int i = 0; i < count; i++) {
            int b = (int) ((src[i] >>> shift) & MASK);
            counts[b]++;
        }

        int total = 0;
        for (int i = 0; i < counts.length; i += 4) {
            int c0 = counts[i];
            int c1 = counts[i + 1];
            int c2 = counts[i + 2];
            int c3 = counts[i + 3];

            counts[i]     = total;
            counts[i + 1] = total + c0;
            counts[i + 2] = total + c0 + c1;
            counts[i + 3] = total + c0 + c1 + c2;

            total += c0 + c1 + c2 + c3;
        }

        for (int i = 0; i < count; i++) {
            long sortKey = src[i];
            int b = (int) ((sortKey >>> shift) & MASK);
            int destIdx = counts[b];
            counts[b] = destIdx + 1;
            dest[destIdx] = sortKey;
        }
    }

}
