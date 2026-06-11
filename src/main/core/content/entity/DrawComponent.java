package core.content.entity;

import core.WorldCoordinates;

public interface DrawComponent {
    // Лучшее решение, которое вообще можно принять.
    // Из-за проблем с неточными числами можно просто 2-3 пикселя отступать и этого даже не будет заметно
    float GAP = WorldCoordinates.INV_BLOCK_SIZE;

    void draw(float dx);
}
