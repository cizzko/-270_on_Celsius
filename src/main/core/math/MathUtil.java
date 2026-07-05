package core.math;

import core.util.Config;
import org.jetbrains.annotations.Range;
import java.lang.annotation.*;
import static java.lang.Byte.toUnsignedInt;

public final class MathUtil {
    private static final String fma = Config.getString("FMAEnabled", "false");
    public static final boolean fmaEnabled = fma.equals("auto") ? fmaTest() : Boolean.parseBoolean(fma);
    //от жита
    private static volatile double garbageSink;

    private MathUtil() {}

    //смешная штука которая буквально в лоб сравнивает фма и обычную математику
    //может давать ложные показания, но в среднем вроде всегда правильно работает
    private static boolean fmaTest() {
        try {
            double localSink = 0;
            final int iterations = 30_000;
            int fmaWinCount = 0;

            for (int round = 0; round < 5; round++) {
                for (int i = 0; i < iterations; i++) {
                    localSink += Math.fma(1.001 + (i & 7), 2.002, 3.003);
                }
                garbageSink = localSink;

                for (int i = 0; i < iterations; i++) {
                    localSink += (1.001 + (i & 7)) * 2.002 + 3.003;
                }
                garbageSink = localSink;

                long startFma = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    localSink += Math.fma(1.001 + (i & 7), 2.002, 3.003);
                }
                garbageSink = localSink;
                long durationFma = System.nanoTime() - startFma;
                long startSimple = System.nanoTime();

                for (int i = 0; i < iterations; i++) {
                    localSink += (1.001 + (i & 7)) * 2.002 + 3.003;
                }
                garbageSink = localSink;
                long durationSimple = System.nanoTime() - startSimple;

                if ((double) durationFma / durationSimple < 1.5) {
                    fmaWinCount++;
                }
            }
            return fmaWinCount >= 3;
        } catch (Throwable t) {
            return false;
        }
    }

    //проблема Math.fma() в том, что если процессор железно не поддерживает фма
    //то мач тащит новый BigDecimal на каждый вызов фма для контракта совместимости
    //старые железки и так задыхаются, а тут им еще десималы крутить
    public static double fma(double a, double b, double c) {
        if (fmaEnabled) {
            return Math.fma(a, b, c);
        }
        return a * b + c;
    }

    //для наших целей погрешность на один бит где то в конце - мелочь
    public static float fma(float a, float b, float c) {
        if (fmaEnabled) {
            return Math.fma(a, b, c);
        }
        return (float) ((double) a * b + c);
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

    public static boolean equalsEps(double a, double b, float eps) {
        return Math.abs(a - b) <= eps;
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
