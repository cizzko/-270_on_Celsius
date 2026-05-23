package core.content.entity;

import core.WorldCoordinates;
import core.math.Rectangle;

import static core.WorldCoordinates.toBlock;

public final class Hitbox {
    public int minX, minY;
    public int maxX, maxY;

    public Hitbox(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public Hitbox() {}

    public Hitbox set(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        return this;
    }

    public Hitbox setRect(float x, float y, float width, float height) {
        this.minX = toBlock(x);
        this.minY = toBlock(y);
        this.maxX = minX + toBlock(width);
        this.maxY = minY + toBlock(height);
        return this;
    }

    public Hitbox set(Rectangle hitbox) {
        this.minX = toBlock(hitbox.x);
        this.minY = toBlock(hitbox.y);
        this.maxX = minX + toBlock(hitbox.width);
        this.maxY = minY + toBlock(hitbox.height);
        return this;
    }

    public Hitbox clamp(int sizeX, int sizeY) {
        this.minX = Math.max(minX, 0);
        this.minY = Math.max(minY, 0);
        this.maxX = Math.min(maxX, sizeX - 1);
        this.maxY = Math.min(maxY, sizeY - 1);
        return this;
    }
}
