package core.entity;

import core.math.Vector2f;

public interface VelocityComponent {

    Vector2f getVelocity();

    void jump(float impulse);

    void moveAt(Vector2f vel);
}
