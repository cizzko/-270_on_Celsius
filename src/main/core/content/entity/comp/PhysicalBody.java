package core.content.entity.comp;

import core.World.Creatures.Physics;
import core.math.MathUtil;
import core.math.Vector2f;

public interface PhysicalBody extends SpatialBody {

    CollisionResult onCollide(PhysicalBody them);

    default boolean isMoved() {
        return
                !MathUtil.equalsEps(lastX(), x(), Physics.EPS) ||
                !MathUtil.equalsEps(lastY(), y(), Physics.EPS);

    }

    void updateLastPosition();
    double lastX();
    double lastY();

    // Блоков / такт
    Vector2f velocity();

    // Блоков / такт²
    Vector2f acceleration();

    float mass();

    enum CollisionResult {
        WALKTHROUGH, // отсутствие взаимодействия
    }
}
