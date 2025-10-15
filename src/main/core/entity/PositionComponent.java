package core.entity;

import core.World.Textures.TextureDrawing;

public interface PositionComponent {
    float getX();
    float getY();

    default int getBlockX() { /* return TextureDrawing.toBlock(getX()); TODO а где метод??? */ throw new UnsupportedOperationException(); }
    default int getBlockY() { /* return TextureDrawing.toBlock(getY()); TODO а где метод??? */ throw new UnsupportedOperationException(); }

    void setPosition(float x, float y);

    void setX(float x);
    void setY(float y);
}
