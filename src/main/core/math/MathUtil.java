package core.math;

public final class MathUtil {
    private MathUtil() {
    }

    public static final Point2i[] CROSS_OFFSETS = {
            new Point2i(0, -1),
            new Point2i(0, +1),
            new Point2i(-1, 0),
            new Point2i(+1, 0),
    };

    public static int ceilNextPowerOfTwo(int v) {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v++;
        return v;
    }

    public static float len(float x, float y) {
        return (float) Math.sqrt(x*x + y*y);
    }
}
