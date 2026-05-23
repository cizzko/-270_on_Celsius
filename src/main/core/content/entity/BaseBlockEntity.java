package core.content.entity;

import core.World.StaticWorldObjects.StaticObjectsConst;

public abstract class BaseBlockEntity<B extends StaticObjectsConst> implements BlockEntity {

    protected int x, y;
    protected final B block;

    protected BaseBlockEntity(B block) {
        this.block = block;
    }

    public final B type() { return block; }

    public int x() { return x; }
    public int y() { return y; }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
