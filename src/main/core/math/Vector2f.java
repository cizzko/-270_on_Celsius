package core.math;

public final class Vector2f {
    public float x, y;

    public Vector2f() {
    }

    public Vector2f(float x, float y) {
        set(x, y);
    }

    public Vector2f(Vector2f other) {
        set(other);
    }

    public Vector2f set(Vector2f other) {
        return set(other.x, other.y);
    }

    public Vector2f set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public float crs(Vector2f other) {
        return x * other.y - y * other.x;
    }

    public Vector2f add(Vector2f other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    public Vector2f sub(Vector2f other) {
        this.x -= other.x;
        this.y -= other.y;
        return this;
    }

    public Vector2f scale(float scalar) {
        x *= scalar;
        y *= scalar;
        return this;
    }

    public Vector2f nor() {
        float len = len();
        if (len != 0) {
            x /= len;
            y /= len;
        }
        return this;
    }

    public float len() {
        return (float) Math.sqrt(len2());
    }

    public float len2() {
        return x * x + y * y;
    }

    public Vector2f mul(Mat3 mat) {
        float rx = x * mat.val[Mat3.M00] + y * mat.val[Mat3.M01] + mat.val[Mat3.M02];
        float ry = x * mat.val[Mat3.M10] + y * mat.val[Mat3.M11] + mat.val[Mat3.M12];
        this.x = rx;
        this.y = ry;

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vector2f other)) {
            return false;
        }
        return Float.compare(x, other.x) == 0 && Float.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Float.hashCode(x);
        h += (h << 5) + Float.hashCode(y);

        return h;
    }
}
