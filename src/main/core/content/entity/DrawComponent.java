package core.content.entity;

import static core.World.Textures.TextureDrawing.blockSize;

public interface DrawComponent {
    // Лучшее решение, которое вообще можно принять.
    // Из-за проблем с неточными числами можно просто 2-3 пикселя отступать и этого даже не будет заметно
    float GAP = 1f / blockSize;

    void draw(float drawX);
}
