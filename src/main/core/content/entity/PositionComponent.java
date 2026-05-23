package core.content.entity;

import core.WorldCoordinates;

public interface PositionComponent {
    float x();
    float y();

    default int blockX() { return WorldCoordinates.toBlock(x()); }
    default int blockY() { return WorldCoordinates.toBlock(y()); }

    void setPosition(float x, float y);

    void setX(float x);
    void setY(float y);

    boolean hasFloor();

    default float dst2(float ox, float oy) {
        float dx = ox - x();
        float dy = oy - y();
        return (dx * dx + dy * dy);
    }

    default boolean within(float ox, float oy, float radius) {
        return dst2(ox, oy) <= radius*radius;
    }
}
