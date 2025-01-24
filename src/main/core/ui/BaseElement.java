package core.ui;

import core.Utils.DebugTools;
import core.Utils.Sized;
import core.math.Point2i;
import core.math.Rectangle;

public abstract class BaseElement<E extends BaseElement<E>> implements Element {
    protected static final int FLAG_X_CHANGED   = 1 << 0;
    protected static final int FLAG_Y_CHANGED   = 1 << 1;
    protected static final int FLAG_W_CHANGED   = 1 << 2;
    protected static final int FLAG_H_CHANGED   = 1 << 3;
    protected static final int FLAG_VISIBLE     = 1 << 4;

    // Допустимая погрешность в координатах интерфейса
    private static final float SIZE_EPSILON = 1e-4f;

    protected static boolean equalsEps(float a, float b) {
        return Math.abs(a - b) < SIZE_EPSILON;
    }

    protected void setFlag(int flag, boolean st) {
        if (st) {
            this.flags |= flag;
        } else {
            this.flags &= ~flag;
        }
    }
    protected void flipFlag(int flag) {
        flags ^= flag;
    }

    public final Group parent;

    protected String id;
    protected float x, y;
    protected float width, height;
    protected int flags = FLAG_VISIBLE;

    protected BaseElement(Group parent) {
        this.parent = parent;
    }

    @SuppressWarnings("unchecked")
    protected E as() {
        return (E) this;
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final Group parent() {
        return parent;
    }

    @Override
    public final float x() {
        return x;
    }

    @Override
    public final float y() {
        return y;
    }

    @Override
    public final float width() {
        return width;
    }

    @Override
    public final float height() {
        return height;
    }

    @Override
    public final boolean visible() {
        return (flags & FLAG_VISIBLE) != 0;
    }

    @Override
    public E setId(String id) {
        this.id = id;
        return as();
    }

    protected void resize() {}
    protected void updateThis() {}

    @Override
    public void update() {
        resize();
        flags &= ~(FLAG_X_CHANGED | FLAG_Y_CHANGED | FLAG_W_CHANGED | FLAG_H_CHANGED);
        updateThis();
    }

    // region Size setters
    @Override
    public final E setWidth(float width) {
        if (!equalsEps(this.width, width)) {
            this.width = width;
            this.flags |= FLAG_W_CHANGED;
        }
        return as();
    }

    @Override
    public final E setHeight(float height) {
        if (!equalsEps(this.height, height)) {
            this.height = height;
            this.flags |= FLAG_H_CHANGED;
        }
        return as();
    }

    @Override
    public final E setX(float x) {
        if (!equalsEps(this.x, x)) {
            this.x = x;
            this.flags |= FLAG_X_CHANGED;
        }
        return as();
    }

    @Override
    public final E setY(float y) {
        if (!equalsEps(this.y, y)) {
            this.y = y;
            this.flags |= FLAG_Y_CHANGED;
        }
        return as();
    }

    // endregion
    // region Size helpers
    @Override
    public final E setPosition(float x, float y) {
        setX(x);
        setY(y);
        return as();
    }

    @Override
    public final E setSize(Sized size) {
        return setSize(size.width(), size.height());
    }
    @Override
    public final E setSize(float width, float height) {
        setWidth(width);
        setHeight(height);
        return as();
    }

    @Override
    public final E set(float x, float y, float width, float height) {
        setPosition(x, y);
        setSize(width, height);
        return as();
    }

    // endregion

    @Override
    public E setVisible(boolean visible) {
        setFlag(FLAG_VISIBLE, visible);
        return as();
    }

    @Override
    public E toggleVisibility() {
        flipFlag(FLAG_VISIBLE);
        return as();
    }

    @Override
    public Element hit(float hx, float hy) {
        return Rectangle.contains(x, y, width, height, hx, hy) ? this : null;
    }

    @Override
    public final String toString() {
        return toStringImpl(0);
    }

    protected String toStringImpl(int indent) {
        String i = id;
        return getClass().getSimpleName() + (i != null ? "<" + i + ">" : "") + printPosition();
    }

    protected String printPosition() {
        return " (" +
                "x=" + DebugTools.FLOATS.format(x) +
                ", y=" + DebugTools.FLOATS.format(y) +
                ", w=" + DebugTools.FLOATS.format(width) +
                ", h=" + DebugTools.FLOATS.format(height) +
                ")";
    }
}
