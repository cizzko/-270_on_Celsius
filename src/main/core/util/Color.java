package core.util;

public final class Color {
    public static final Color WHITE = new Color(0xFFFFFFFF);
    public static final Color BLACK = new Color(0x000000FF);
    public static final Color CLEAR = new Color(0x00000000);

    private int rgba8888;

    public Color() {}

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
        return (r & 0xff) << 24 | (g & 0xff) << 16 | (b & 0xff) << 8 | (a & 0xff);
    }

    public static int argbToRgba8888(int argb8888) {
        return Integer.rotateRight(argb8888, 24);
    }

    public static Color fromRgba8888(int r, int g, int b, int a) {
        return new Color(r, g, b, a);
    }

    public static float toGLBits(int rgba8888) {
        return Float.intBitsToFloat(Integer.reverseBytes(rgba8888) & 0xfeffffff);
    }

    public static String toString(int rgba8888) {
        int zeros = Integer.numberOfLeadingZeros(rgba8888);
        return "0".repeat(zeros / 4) + Integer.toHexString(rgba8888);
    }

    public float toGLBits() { return toGLBits(rgba8888); }
    public int rgba8888() { return rgba8888; }

    public int r() { return rgba8888 >> 24 & 0xff; }
    public int g() { return rgba8888 >> 16 & 0xFF; }
    public int b() { return rgba8888 >> 8 & 0xFF; }
    public int a() { return rgba8888 & 0xFF; }

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
