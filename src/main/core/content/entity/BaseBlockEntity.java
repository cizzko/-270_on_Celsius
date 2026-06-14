package core.content.entity;

import core.content.blocks.Block;
import core.math.MathUtil;

public abstract class BaseBlockEntity<B extends Block> implements BlockEntity {

    protected final B block;
    protected short x, y;

    protected BaseBlockEntity(B block) {
        this.block = block;
    }

    public final B type() { return block; }

    public final short x() { return x; }
    public final short y() { return y; }

    public final void setPosition(int x, int y) {
        this.x = MathUtil.toShortExact(x);
        this.y = MathUtil.toShortExact(y);
    }
}
