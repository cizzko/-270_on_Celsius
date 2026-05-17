package core.content.entity;

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
    public final boolean hasFloor() {
        return true;
    }

    @Override
    public float x() {
        return x;
    }

    @Override
    public float y() {
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
