package core.content.entity.comp;

import core.math.Vector2f;

public interface PhysicalBody extends SpatialBody {

    CollisionResult onCollide(PhysicalBody them);

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
