package core.content.entity.comp;

import core.content.blocks.Block;
import core.math.AABB;
import core.math.TmpShapes;
import core.math.Vector2d;

import static core.Global.world;
import static core.WorldCoordinates.toBlock;
import static core.content.entity.comp.DrawComponent.GAP;

public interface SpatialBody {
    double x();
    double y();

    float width();
    float height();

    default Vector2d posTo(Vector2d out) {
        return out.set(x(), y());
    }

    default short blockX() { return toBlock(x()); }
    default short blockY() { return toBlock(y()); }

    default double centerX() { return x() + width()/2d; }
    default double centerY() { return y() + height()/2d; }

    default float offsetX() { return (float)(x() - blockX()); }
    default float offsetY() { return (float)(y() - blockY()); }

    void setPosition(double x, double y);
    void mirrorX(double dx);
    void setY(double y);

    void hitboxTo(AABB out);

    default boolean hasFloor() {
        var hitbox = TmpShapes.aabb1;
        hitboxTo(hitbox);
        hitbox.maxY = hitbox.minY;
        hitbox.minY -= GAP;
        hitbox.maxX -= GAP;
        hitbox.minX += GAP;

        short minX = hitbox.blockMinX();
        short maxX = hitbox.blockMaxX();
        short minY = hitbox.blockMinY();
        short maxY = hitbox.blockMaxY();

        for (; minY <= maxY; minY++) {
            for (short x = minX; x <= maxX; x++) {
                var block = world.getBlock(x, minY);
                if (block == null || block.type == Block.Type.SOLID) {
                    return true;
                }
            }
        }
        return false;
    }

    default double dstSq(SpatialBody b) { return dstSq(b.x(), b.y()); }
    default double dstSq(double x, double y) {
        double dx = x - x();
        double dy = y - y();
        return dx * dx + dy * dy;
    }

    default boolean within(double x, double y, double radius) {
        return dstSq(x, y) <= radius*radius;
    }
}
