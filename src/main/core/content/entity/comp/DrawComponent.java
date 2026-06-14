package core.content.entity.comp;

import core.WorldCoordinates;
import core.math.AABB;

public interface DrawComponent {
    // Лучшее решение, которое вообще можно принять.
    // Из-за проблем с неточными числами можно просто 2-3 пикселя отступать и этого даже не будет заметно
    float GAP = WorldCoordinates.INV_BLOCK_SIZE;

    boolean isVisible(AABB viewport);

    void draw(float dx);
}
