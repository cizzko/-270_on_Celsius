package core.Utils;

import java.io.Serializable;

public final class SimpleColor implements Serializable {
    public static final SimpleColor WHITE = fromRGBA(255, 255, 255, 255);
    public static final SimpleColor BLACK = fromRGBA(0, 0, 0, 0);

    private int value; // argb

    private static int clamp(int c) {
        return Math.clamp(c, 0, 255);
    }

    public SimpleColor(int r, int g, int b, int a) {
        r = clamp(r);
        g = clamp(g);
        b = clamp(b);
        a = clamp(a);
        value = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF));
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
