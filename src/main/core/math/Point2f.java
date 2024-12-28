package core.math;

public final class Point2f {
    public float x, y;

    public Point2f() {}
    public Point2f(float x, float y) {
        set(x, y);
    }
    public Point2f(Point2f other) {
        set(other);
    }

    public Point2f set(float x, float y) {
        this.x = x;
        this.y = y;

        return this;
    }

    public Point2f set(Point2f other) {
        return set(other.x, other.y);
    }

    public Point2f add(float x, float y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public Point2f add(Point2f other) {
        return add(other.x, other.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Point2f point)) {
            return false;
        }
        return x == point.x && y == point.y;
    }

    @Override
    public String toString(){
        return "(" + x + ", " + y + ")";
    }
}
