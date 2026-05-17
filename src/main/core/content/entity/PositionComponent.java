package core.content.entity;

import core.World.Textures.TextureDrawing;

public interface PositionComponent {
    float x();
    float y();

    default int blockX() { return TextureDrawing.toBlock(x()); }
    default int blockY() { return TextureDrawing.toBlock(y()); }

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

    default boolean withinBlocks(int ox, int oy, int radius) {
        long dx = ox - blockX();
        long dy = oy - blockY();
        long radiusSq = (long) radius * radius;
        return (dx * dx + dy * dy) <= radiusSq;
    }
}
