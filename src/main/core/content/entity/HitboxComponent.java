package core.content.entity;

import core.math.AABB;

public interface HitboxComponent extends PositionComponent {

    double centerX();
    double centerY();

    void hitboxTo(AABB out);

    CollisionResult onCollide(HitboxComponent them);

    void updateLastPosition();
    double lastX();
    double lastY();

    // boundingbox или типа того
    float width();
    float height();

    enum CollisionResult {
        WALKTHROUGH, // отсутствие взаимодействия
    }
}
