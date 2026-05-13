package core.entity;

import core.World.StaticWorldObjects.StaticObjectsConst;

public abstract class BaseBlockEntity<B extends StaticObjectsConst> implements BlockEntity {

    protected float x, y;
    protected final B block;

    protected BaseBlockEntity(B block) {
        this.block = block;
    }

    @Override
    public final B getBlock() {
        return block;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void setX(float x) {
        this.x = x;
    }

    @Override
    public void setY(float y) {
        this.y = y;
    }

    @Override
    public void draw() {
    }
}
