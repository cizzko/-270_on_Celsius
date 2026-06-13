package core.math;

public final class Vector2d {
    public double x, y;

    public double x() {
        return x;
    }
    public double y() {
        return y;
    }

    public float xf() { return (float) x; }
    public float yf() { return (float) y; }

    public Vector2d set(double v) {
        x = v;
        y = v;
        return this;
    }

    public Vector2d set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2d add(double x, double y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public Vector2d add(Vector2d v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    public Vector2d scale(double v) {
        this.x *= v;
        this.y *= v;
        return this;
    }

    public Vector2d sub(double x, double y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public Vector2d sub(Vector2d v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }

    public double len() {
        return Math.sqrt(x * x + y * y);
    }

    public double lenSq() {
        return x * x + y * y;
    }

    public Vector2d nor() {
        double len = len();
        if (len != 0) {
            x /= len;
            y /= len;
        }
        return this;
    }

    public Vector2d floor() {
        x = Math.floor(x);
        y = Math.floor(y);
        return this;
    }


    public Vector2d lerp(double tx, double ty, double t) {
        x = MathUtil.lerp(x, tx, t);
        y = MathUtil.lerp(y, ty, t);
        return this;
    }

    public boolean isFinite() {
        return Double.isFinite(x) && Double.isFinite(y);
    }

    public String toString() {
        return '{' + x + ", " + y + '}';
    }
}
