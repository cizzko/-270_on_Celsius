package core.ui;

import core.Utils.Sized;
import core.math.Point2i;
import core.math.Rectangle;

public abstract class BaseElement<E extends BaseElement<E>> implements Element {
    public final Group parent;

    protected float x, y;
    protected float width, height;
    protected boolean visible = true;

    protected BaseElement(Group parent) {
        this.parent = parent;
    }

    @SuppressWarnings("unchecked")
    protected E as() {
        return (E) this;
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
        return visible;
    }

    @Override
    public E setSize(float size) {
        this.width = size;
        this.height = size;
        return as();
    }

    @Override
    public E setSize(Sized size) {
        this.width = size.width();
        this.height = size.height();
        return as();
    }

    @Override
    public E setSize(float width, float height) {
        this.width = width;
        this.height = height;
        return as();
    }

    @Override
    public E set(float x, float y, float width, float height) {
        setPosition(x, y);
        setSize(width, height);
        return as();
    }

    @Override
    public E setX(float x) {
        this.x = x;
        return as();
    }

    @Override
    public E setY(float y) {
        this.y = y;
        return as();
    }

    @Override
    public E setPosition(float x, float y) {
        setX(x);
        setY(y);
        return as();
    }

    @Override
    public E setPosition(Point2i position) {
        setPosition(position.x, position.y);
        return as();
    }

    @Override
    public E setVisible(boolean visible) {
        this.visible = visible;
        return as();
    }

    @Override
    public E toggleVisibility() {
        visible = !visible;
        return as();
    }

    @Override
    public Element hit(float hx, float hy) {
        return Rectangle.contains(x, y, width, height, hx, hy) ? this : null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + printPosition();
    }

    protected String printPosition() {
        return "(" + "x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ")";
    }
}
