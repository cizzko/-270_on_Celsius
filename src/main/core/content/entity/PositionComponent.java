package core.content.entity;

import core.math.Vector2d;

public interface PositionComponent {
    double x();
    double y();

    default Vector2d posTo(Vector2d out) {
        return out.set(x(), y());
    }

    short blockX();
    short blockY();

    float offsetX();
    float offsetY();

    void setPosition(double x, double y);
    void setX(double x);
    void setY(double y);

    boolean hasFloor();

    default double dstSq(PositionComponent b) { return dstSq(b.x(), b.y()); }
    double dstSq(double x, double y);

    default boolean within(double x, double y, double radius) {
        return dstSq(x, y) <= radius*radius;
    }
}
