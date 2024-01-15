package core.Utils;

import java.io.Serializable;

public final class SimpleColor implements Serializable {
    public static final SimpleColor WHITE = fromRGBA(255, 255, 255, 255);
    public static final SimpleColor CLEAR = fromRGBA(0, 0, 0, 0);
    public static final SimpleColor DEFAULT_ORANGE = fromRGBA(255, 80, 0, 55);
    public static final SimpleColor DIRTY_WHITE = fromRGBA(230, 230, 230, 55);
    public static final SimpleColor DIRTY_BRIGHT_WHITE = fromRGBA(230, 230, 230, 255);
    public static final SimpleColor DIRTY_BRIGHT_BLACK = fromRGBA(10, 10, 10, 255);
    public static final SimpleColor DIRTY_BLACK = fromRGBA(10, 10, 10, 55);

    private final int value; // argb?

    public static SimpleColor toColor(int value) {
        return SimpleColor.fromRGBA((value >> 16) & 0xFF, (value >> 8) & 0xFF, (value) & 0xFF, (value >> 24) & 0xff);
    }

    private static int clamp(int value) {
        return Math.clamp(value, 0, 255);
    }

    private SimpleColor(int r, int g, int b, int a) {
        r = clamp(r);
        g = clamp(g);
        b = clamp(b);
        a = clamp(a);
        value = (a << 24) | (r << 16) | (g << 8) | (b);
    }

    public static int argbToRgba(int argb) {
        return  ((argb & 0x00FF0000) >> 16) |
                ((argb & 0x0000FF00)) |
                ((argb & 0x000000FF) << 16) |
                ((argb & 0xFF000000));
    }

    public static SimpleColor fromRGBA(int r, int g, int b, int a) {
        return new SimpleColor(r, g, b, a);
    }

    public float toABGRBits() {
        return Float.intBitsToFloat(getValueABGR() & 0xfeffffff);
    }

    public int getValueARGB() {
        return value;
    }

    public int getValueRGBA() {
        return (getRed() << 24) | (getGreen() << 16) | (getBlue() << 8) | getAlpha();
    }

    public int getValueABGR() {
        return (getAlpha() << 24) | (getBlue() << 16) | (getGreen() << 8) | getRed();
    }

    public int getRed() {
        return (value >> 16) & 0xFF;
    }

    public int getGreen() {
        return (value >> 8) & 0xFF;
    }

    public int getBlue() {
        return (value) & 0xFF;
    }

    public int getAlpha() {
        return (value >> 24) & 0xff;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SimpleColor that && value == that.value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
