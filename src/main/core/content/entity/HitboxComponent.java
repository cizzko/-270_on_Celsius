package core.content.entity;

import core.math.AABB;

public interface HitboxComponent extends PositionComponent {

    double centerX();
    double centerY();

    void hitboxTo(AABB out);

    CollisionResult onCollide(HitboxComponent them);

    enum CollisionResult {
        WALKTHROUGH, // отсутствие взаимодействия
    }
}
