package core.content.entity;

import core.World.Textures.TextureDrawing;

public interface PositionComponent {
    float getX();
    float getY();

    default int getBlockX() { return TextureDrawing.toBlock(getX()); }
    default int getBlockY() { return TextureDrawing.toBlock(getY()); }

    void setPosition(float x, float y);

    void setX(float x);
    void setY(float y);

    boolean hasFloor();

    default float dst2(float ox, float oy) {
        float dx = ox - getX();
        float dy = oy - getY();
        return (dx * dx + dy * dy);
    }

    default boolean within(float ox, float oy, float radius) {
        return dst2(ox, oy) <= radius*radius;
    }

    default boolean withinBlocks(int ox, int oy, int radius) {
        long dx = ox - getBlockX();
        long dy = oy - getBlockY();
        long radiusSq = (long) radius * radius;
        return (dx * dx + dy * dy) <= radiusSq;
    }
}
