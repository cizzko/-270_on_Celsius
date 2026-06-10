package core.content.entity;

import core.content.blocks.Block;

public abstract class BaseBlockEntity<B extends Block> implements BlockEntity {

    protected short x, y;
    protected final B block;

    protected BaseBlockEntity(B block) {
        this.block = block;
    }

    public final B type() { return block; }

    public short x() { return x; }
    public short y() { return y; }

    public void setPosition(short x, short y) {
        this.x = x;
        this.y = y;
    }
}
