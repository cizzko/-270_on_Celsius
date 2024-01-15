package core.math;

public final class Point2i {
    public int x, y;

    public Point2i() {}
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Point2i point2i)) {
            return false;
        }
        return x == point2i.x && y == point2i.y;
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + x;
        h += (h << 5) + y;
        return h;
    }

    @Override
    public String toString(){
        return "(" + x + ", " + y + ")";
    }
}
