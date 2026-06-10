package core.content.entity;

import core.math.Vector2f;

public interface VelocityComponent {

    // Блоков / такт
    Vector2f velocity();

    // Блоков / такт²
    Vector2f acceleration();
}
