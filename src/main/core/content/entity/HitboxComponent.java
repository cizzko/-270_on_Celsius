package core.content.entity;

import core.math.Rectangle;

public interface HitboxComponent extends PositionComponent {

    float prevX();
    float prevY();

    void getHitboxTo(Rectangle out);

    CollisionResult onCollide(HitboxComponent them);

    enum CollisionResult {
        RESISTANT,   // смещение в мире (отталкивание)
        WALKTHROUGH,
        ; // отсутствие взаимодействия

        public CollisionResult combine(CollisionResult other) {
            return this == RESISTANT || other == RESISTANT ? this : other;
        }
    }
}
