package core.math;

import org.jetbrains.annotations.Range;

import java.lang.annotation.*;

import static java.lang.Byte.toUnsignedInt;

public final class MathUtil {
    private MathUtil() {
    }

    public static final Point2i[] CROSS_OFFSETS = {
            new Point2i(0, -1),
            new Point2i(0, +1),
            new Point2i(-1, 0),
            new Point2i(+1, 0),
    };

    public static final float FLOAT_EPSILON = Math.ulp(1f);
    public static final float EPSILON       = 1e-5f;

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

    public static float len2(float x, float y) {
        return x * x + y * y;
    }

    public static float lerp(float a, float b, float t) {
        return Math.fma(b - a, t, a);
    }

    public static double lerp(double a, double b, double t) {
        return Math.fma(b - a, t, a);
    }

    public static byte toByteExact(int value) {
        if ((byte)value != value) {
            throw new ArithmeticException("byte overflow");
        }
        return (byte)value;
    }

    public static short toShortExact(int value) {
        if ((short)value != value) {
            throw new ArithmeticException("short overflow");
        }
        return (short)value;
    }

    public static boolean equalsEps(float a, float b) {
        return equalsEps(a, b, EPSILON);
    }

    public static boolean equalsEps(float a, float b, float eps) {
        return Math.abs(a - b) <= eps;
    }

    public static byte incrementExact(byte b) {
        if (b == Byte.MAX_VALUE) {
            throw new ArithmeticException("byte overflow");
        }
        return (byte)(b + 1);
    }

    public static byte decrementExact(byte b) {
        if (b == Byte.MIN_VALUE) {
            throw new ArithmeticException("byte overflow");
        }
        return (byte)(b - 1);
    }

    public static byte addExact(byte a, byte b) {
        return toByteExact(Math.addExact(toUnsignedInt(a), toUnsignedInt(b)));
    }

    public static float cos(double v) { return (float) Math.cos(v); }
    public static float sin(double v) { return (float) Math.sin(v); }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.TYPE_USE})
    @Range(from = 0, to = (1 << 8))
    public @interface UnsignedByte {

    }
}
