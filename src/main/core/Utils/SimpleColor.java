package core.Utils;

import java.io.Serializable;
import java.util.Locale;

public final class SimpleColor implements Serializable {
    public static final SimpleColor WHITE = new SimpleColor(0xFFFFFFFF);
    public static final SimpleColor CLEAR = new SimpleColor(0x00000000);

    public static final SimpleColor DEFAULT_ORANGE = fromRGBA(255, 80, 0, 55);
    public static final SimpleColor DIRTY_WHITE = fromRGBA(230, 230, 230, 55);
    public static final SimpleColor DIRTY_BRIGHT_WHITE = fromRGBA(230, 230, 230, 255);
    public static final SimpleColor DIRTY_BRIGHT_BLACK = fromRGBA(10, 10, 10, 255);
    public static final SimpleColor DIRTY_BLACK = fromRGBA(10, 10, 10, 55);
    public static final SimpleColor TEXT_COLOR = fromRGBA(210, 210, 210, 255);

    public int rgba;

    private static int clamp(int value) {
        return Math.clamp(value, 0, 255);
    }

    public SimpleColor() {}

    public SimpleColor(int rgba) {
        this.rgba = rgba;
    }

    private SimpleColor(int r, int g, int b, int a) {
        r = clamp(r);
        g = clamp(g);
        b = clamp(b);
        a = clamp(a);
        rgba = __rgba(r, g, b, a);
    }

    private static int __rgba(int r, int g, int b, int a) {
        return (r << 24) | (g << 16) | (b << 8) | (a);
    }

    public static int rgba(int r, int g, int b, int a) {
        r = clamp(r);
        g = clamp(g);
        b = clamp(b);
        a = clamp(a);
        return __rgba(r, g, b, a);
    }

    public static int argbToRgba(int argb) {
        return Integer.rotateRight(argb, 24);
    }

    public static SimpleColor fromRGBA(int r, int g, int b, int a) {
        return new SimpleColor(r, g, b, a);
    }

    public float toGLBits() {
        return Float.intBitsToFloat(Integer.reverseBytes(rgba) & 0xfeffffff);
    }

    public int getValueRGBA() {
        return rgba;
    }

    public int getRed() {
        return (rgba >> 24) & 0xff;
    }

    public int getGreen() {
        return (rgba >> 16) & 0xFF;
    }

    public int getBlue() {
        return (rgba >> 8) & 0xFF;
    }

    public int getAlpha() {
        return (rgba) & 0xFF;
    }

    public void setRed(int value) {
        int r = clamp(value);
        this.rgba = (r << 24) | (getGreen() << 16) | (getBlue() << 8) | getAlpha();
    }

    public void setGreen(int value) {
        int g = clamp(value);
        this.rgba = (getRed() << 24) | (g << 16) | (getBlue() << 8) | getAlpha();
    }

    public void setBlue(int value) {
        int b = clamp(value);
        this.rgba = (getRed() << 24) | (getGreen() << 16) | (b << 8) | getAlpha();
    }

    public void setAlpha(int value) {
        int a = clamp(value);
        this.rgba = (getRed() << 24) | (getGreen() << 16) | (getBlue() << 8) | a;
    }

    public void setRGBA(int r, int g, int b, int a) {
        r = clamp(r);
        g = clamp(g);
        b = clamp(b);
        a = clamp(a);
        this.rgba = __rgba(r, g, b, a);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SimpleColor that && rgba == that.rgba;
    }

    @Override
    public int hashCode() {
        return rgba;
    }

    @Override
    public String toString() {
        int zeros = Integer.numberOfLeadingZeros(rgba);
        return "0".repeat(zeros / 4) + Integer.toHexString(rgba);
    }
}
