package core.graphic;

import java.util.concurrent.ThreadLocalRandom;

import static core.graphic.Colorf.clamp;

public final class Color {
    public static final int red   = 0xff0000ff;
    public static final int green = 0x00ff00ff;
    public static final int blue  = 0x0000ffff;

    public static final int white = 0xffffffff;
    public static final int black = 0x000000ff;
    public static final int clear = 0x00000000;

    public static final Color WHITE = new Color(white);
    public static final Color BLACK = new Color(black);
    public static final Color CLEAR = new Color(clear);

    private int rgba8888;

    public Color() {}

    public Color(Colorf color) { this.rgba8888 = color.rgba8888(); }

    public Color(Color color) {
        this.rgba8888 = color.rgba8888;
    }

    public Color(int rgba8888) {
        this.rgba8888 = rgba8888;
    }

    public Color(int r, int g, int b, int a) {
        this.rgba8888 = rgba8888(r, g, b, a);
    }

    public static int rgba8888(int r, int g, int b, int a) {
        return Math.clamp(r, 0, 255) << 24 | Math.clamp(g, 0, 255) << 16 | Math.clamp(b, 0, 255) << 8 | Math.clamp(a, 0, 255);
    }

    public static int argbToRgba8888(int argb8888) {
        return Integer.rotateRight(argb8888, 24);
    }

    public static Color fromRgba8888(int r, int g, int b, int a) {
        return new Color(r, g, b, a);
    }

    public static float toGLBits(int rgba8888) {
        return Float.intBitsToFloat(Integer.reverseBytes(rgba8888) & 0xfffeffff);
    }

    public static String toString(int rgba8888) {
        int zeros = Integer.numberOfLeadingZeros(rgba8888);
        return "0".repeat(zeros / 4) + Integer.toHexString(rgba8888);
    }

    public static int random() {
        return rgba8888(
                ThreadLocalRandom.current().nextInt(0, 255),
                ThreadLocalRandom.current().nextInt(0, 255),
                ThreadLocalRandom.current().nextInt(0, 255),
                255);
    }

    public static int withR(int rgba8888, int newR) { return (Math.clamp(newR, 0, 255) << 24) | (rgba8888 & 0x00FFFFFF); }
    public static int withG(int rgba8888, int newG) { return (rgba8888 & 0xFF00FFFF) | (Math.clamp(newG, 0, 255) << 16); }
    public static int withB(int rgba8888, int newB) { return (rgba8888 & 0xFFFF00FF) | (Math.clamp(newB, 0, 255) << 8); }
    public static int withA(int rgba8888, int newA) { return (rgba8888 & 0xFFFFFF00) | Math.clamp(newA, 0, 255); }

    public float toGLBits() { return toGLBits(rgba8888); }
    public int rgba8888() { return rgba8888; }

    public int r() { return rgba8888 >> 24 & 0xff; }
    public int g() { return rgba8888 >> 16 & 0xFF; }
    public int b() { return rgba8888 >> 8 & 0xFF; }
    public int a() { return rgba8888 & 0xFF; }

    public int rgb() { return rgba8888 >> 8 & 0xFFFFFF; }

    public float rf() { return r() / 255f; }
    public float gf() { return g() / 255f; }
    public float bf() { return b() / 255f; }
    public float af() { return a() / 255f; }

    public void r(int r) { this.rgba8888 = rgba8888(r, g(), b(), a()); }
    public void g(int g) { this.rgba8888 = rgba8888(r(), g, b(), a()); }
    public void b(int b) { this.rgba8888 = rgba8888(r(), g(), b, a()); }
    public void a(int a) { this.rgba8888 = rgba8888(r(), g(), b(), a); }

    public void set(int r, int g, int b, int a) {
        this.rgba8888 = rgba8888(r, g, b, a);
    }
    public void set(Color color) { this.rgba8888 = color.rgba8888; }
    public void setRgba8888(int rgba8888) { this.rgba8888 = rgba8888; }

    public void add(Color color) { this.rgba8888 = rgba8888(r() + color.r(), g() + color.g(), b() + color.b(), a() + color.a()); }
    public void sub(Color color) { this.rgba8888 = rgba8888(r() - color.r(), g() - color.g(), b() - color.b(), a() - color.a()); }
    public void mul(Color color) { this.rgba8888 = rgba8888(r() * color.r(), g() * color.g(), b() * color.b(), a() * color.a()); }
    public void div(Color color) { this.rgba8888 = rgba8888(r() / color.r(), g() / color.g(), b() / color.b(), a() / color.a()); }

    static int toInt(float c) { return (int) (clamp(c) * 255); }
    public void rf(float r) { this.rgba8888 = rgba8888(toInt(r), g(), b(), a()); }
    public void gf(float g) { this.rgba8888 = rgba8888(r(), toInt(g), b(), a()); }
    public void bf(float b) { this.rgba8888 = rgba8888(r(), g(), toInt(b), a()); }
    public void af(float a) { this.rgba8888 = rgba8888(r(), g(), b(), toInt(a)); }

    public Colorf copyf() { return new Colorf(this); }
    public Color  copyi() { return new Color(this); }
    public Color  copy() { return new Color(this); }

    public boolean equals(Color o) { return rgba8888 == o.rgba8888; }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Color that && rgba8888 == that.rgba8888;
    }

    @Override
    public int hashCode() {
        return rgba8888;
    }

    @Override
    public String toString() {
        return toString(rgba8888);
    }
}
