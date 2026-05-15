package core.UI;

import core.Application;
import core.Global;
import core.input.InputListener;
import core.math.Rectangle;
import core.util.DebugTools;
import core.util.Sized;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

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
    protected ArrayList<InputListener> inputListeners = new ArrayList<>();

    protected BaseElement(Group parent) {
        this.parent = parent;
    }

    public void addListener(InputListener listener) {
        this.inputListeners.add(listener);
    }

    @Override
    public final boolean remove() {
        return parent != null && parent.remove(this);
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
    protected void updateThis(float dt) {}

    @Override
    public void update(float dt) {
        resize();
        flags &= ~(FLAG_X_CHANGED | FLAG_Y_CHANGED | FLAG_W_CHANGED | FLAG_H_CHANGED);
        updateThis(dt);
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
    public @Nullable Element hit(float hx, float hy) {
        return Rectangle.contains(x, y, width, height, hx, hy) ? this : null;
    }



    // region InputListener
    @Override
    public final boolean onTouchDown(float x, float y, int button) {
        boolean anyHandled = false;
        for (InputListener listener : inputListeners) {
            if (listener.onTouchDown(x, y, button)) {
                Global.uiScene.setTouchFocus(this);
                anyHandled = true;
            }
        }
        return anyHandled;
    }

    @Override
    public final void onTouchUp(float x, float y, int button) {
        inputListeners.forEach(listener -> listener.onTouchUp(x, y, button));
    }

    @Override
    public final void onScroll(float xOffset, float yOffset) {
        inputListeners.forEach(listener -> listener.onScroll(xOffset, yOffset));
    }

    @Override
    public final void onMouseMove(float x, float y) {
        inputListeners.forEach(listener -> listener.onMouseMove(x, y));
    }

    @Override
    public final void onMouseDragged(float x, float y) {
        inputListeners.forEach(listener -> listener.onMouseDragged(x, y));
    }

    @Override
    public final void onKeyUp(int key, int scancode) {
        inputListeners.forEach(listener -> listener.onKeyUp(key, scancode));
    }

    @Override
    public final void onKeyDown(int key, int scancode) {
        inputListeners.forEach(listener -> listener.onKeyDown(key, scancode));
    }

    @Override
    public final void onKeyRepeat(int key, int scancode) {
        inputListeners.forEach(listener -> listener.onKeyRepeat(key, scancode));
    }

    @Override
    public final void onCodepoint(int codepoint) {
        inputListeners.forEach(listener -> listener.onCodepoint(codepoint));
    }

    @Override
    public final void onMouseEnter(float x, float y) {
        inputListeners.forEach(listener -> listener.onMouseEnter(x, y));
    }

    @Override
    public final void onMouseExit(float x, float y) {
        inputListeners.forEach(listener -> listener.onMouseExit(x, y));
    }
    // endregion

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
