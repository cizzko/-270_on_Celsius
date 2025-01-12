package core.math;

public final class Rectangle {
    public float x, y;
    public float width, height;

    public boolean overlaps(Rectangle rect) {
        return x <= rect.x + rect.width && x + width >= rect.x && y <= rect.y + rect.height && y + height >= rect.y;
    }

    public Vector2f getCenterTo(Vector2f vector) {
        vector.x = x + width / 2;
        vector.y = y + height / 2;
        return vector;
    }

    public boolean contains(float rx, float ry, float rwidth, float rheight) {
        return x <= rx + rwidth && x + width >= rx && y <= ry + rheight && y + height >= ry;
    }

    public boolean contains(float px, float py) {
        return x <= px && x + width >= px && y <= py && y + height >= py;
    }

    public static boolean contains(float x, float y, float width, float height,
                                   float px, float py) {
        return x <= px && x + width >= px && y <= py && y + height >= py;
    }

    public static boolean contains(int x, int y, int width, int height, Point2i point) {
        return contains(x, y, width, height, point.x, point.y);
    }

    public void set(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle setSize(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public Rectangle setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Rectangle setCenter(float x, float y) {
        setPosition(x - width / 2, y - height / 2);
        return this;
    }

    @Override
    public String toString() {
        return "Rectangle[" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ']';
    }
}
