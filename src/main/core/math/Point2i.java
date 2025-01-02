package core.math;

public final class Point2i {
    public int x, y;

    public Point2i() {
    }

    public Point2i(int x, int y) {
        set(x, y);
    }

    public Point2i(Point2i other) {
        set(other);
    }

    public Point2i set(int x, int y) {
        this.x = x;
        this.y = y;

        return this;
    }

    public Point2i set(Point2i other) {
        return set(other.x, other.y);
    }

    public Point2i add(int x, int y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public Point2i add(Point2i other) {
        return add(other.x, other.y);
    }

    public Point2i copy() {
        return new Point2i(x, y);
    }

    public float dst2(int x, int y) {
        int dx = x - this.x;
        int dy = y - this.y;
        return dx * dx + dy * dy;
    }

    public float dst2(Point2i other) {
        return dst2(other.x, other.y);
    }

    public float dst(Point2i other) {
        return (float) Math.sqrt(dst2(other));
    }

    public boolean within(int x, int y, int dst) {
        return dst2(x, y) <= dst * dst;
    }

    public boolean within(Point2i other, int dst) {
        return dst2(other) <= dst * dst;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Point2i point)) {
            return false;
        }
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + x;
        h += (h << 5) + y;
        return h;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
