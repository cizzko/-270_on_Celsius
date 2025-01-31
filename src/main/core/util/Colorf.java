package core.util;

public final class Colorf {
    private float r, g, b, a;

    static float clamp(float val) {
        return Math.clamp(val, 0, 1);
    }

    public Colorf() {}

    public Colorf(Colorf color) {
        set(color);
    }

    public Colorf(float r, float g, float b, float a) {
        set(r, g, b, a);
    }

    public float toGlBits() {
        return Color.toGLBits(rgba8888());
    }

    public int rgba8888() {
        return Color.rgba8888(ri(), gi(), bi(), ai());
    }

    public void r(float r) { this.r = clamp(r); }
    public void g(float g) { this.g = clamp(g); }
    public void b(float b) { this.b = clamp(b); }
    public void a(float a) { this.a = clamp(a); }

    public void ri(int r) { this.r = r / 255f; }
    public void gi(int g) { this.g = g / 255f; }
    public void bi(int b) { this.b = b / 255f; }
    public void ai(int a) { this.a = a / 255f; }

    public void set(Colorf color) {
        this.r = color.r;
        this.g = color.g;
        this.b = color.b;
        this.a = color.a;
    }

    public void set(Color color) {
        this.r = color.r() / 255f;
        this.g = color.g() / 255f;
        this.b = color.b() / 255f;
        this.a = color.a() / 255f;
    }

    public void set(float r, float g, float b, float a) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
    }

    public void seti(int r, int g, int b, int a) {
        this.r = r / 255f;
        this.g = g / 255f;
        this.b = b / 255f;
        this.a = a / 255f;
    }

    public void add(float r, float g, float b, float a) {
        this.r = clamp(this.r + r);
        this.g = clamp(this.g + g);
        this.b = clamp(this.b + b);
        this.a = clamp(this.a + a);
    }

    public void sub(float r, float g, float b, float a) {
        this.r = clamp(this.r - r);
        this.g = clamp(this.g - g);
        this.b = clamp(this.b - b);
        this.a = clamp(this.a - a);
    }

    public void div(float r, float g, float b, float a) {
        this.r = clamp(this.r / r);
        this.g = clamp(this.g / g);
        this.b = clamp(this.b / b);
        this.a = clamp(this.a / a);
    }

    public void mul(float r, float g, float b, float a) {
        this.r = clamp(this.r * r);
        this.g = clamp(this.g * g);
        this.b = clamp(this.b * b);
        this.a = clamp(this.a * a);
    }

    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }
    public float a() { return a; }

    public int ri() { return (int) (r * 255f); }
    public int gi() { return (int) (g * 255f); }
    public int bi() { return (int) (b * 255f); }
    public int ai() { return (int) (a * 255f); }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Colorf colorf && rgba8888() == colorf.rgba8888();
    }

    @Override
    public int hashCode() {
        return rgba8888();
    }

    @Override
    public String toString() {
        return Color.toString(rgba8888());
    }
}
