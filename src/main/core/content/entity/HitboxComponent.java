package core.content.entity;

import core.math.Rectangle;

public interface HitboxComponent extends PositionComponent {

    float prevX();
    float prevY();

    float centerX();
    float centerY();

    void getHitboxTo(Rectangle out);

    CollisionResult onCollide(HitboxComponent them);

    enum CollisionResult {
        WALKTHROUGH, // отсутствие взаимодействия
    }
}
