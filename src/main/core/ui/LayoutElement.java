package core.ui;

import core.ui.animation.AlphaAction;
import core.graphic.Color;
import core.input.InputListener;
import core.math.MathUtil;
import core.math.Rectangle;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.List;


// |         toppad       toppad            |
// | leftpad [Child1] gap [Child2] rightpad |
// |         bottompad    bottompad         |
public abstract class LayoutElement<This extends LayoutElement<This>>
        implements Element, AlphaAction.Colored {

    protected static final int FLAG_X_CHANGED        = 1 << 0;
    protected static final int FLAG_Y_CHANGED        = 1 << 1;
    protected static final int FLAG_W_CHANGED        = 1 << 2;
    protected static final int FLAG_H_CHANGED        = 1 << 3;
    /// Если `false`, то рендер элемента пропускается
    protected static final int FLAG_VISIBLE          = 1 << 4;
    /// Если `false`, то элемент не будет реагировать на [#hit] и фокус не будет настроен на него
    protected static final int FLAG_TOUCHABLE        = 1 << 5;
    protected static final int FLAG_FILL_PARENT      = 1 << 6;
    protected static final int FLAG_HORIZONTAL_CLIP  = 1 << 7;
    protected static final int FLAG_VERTICAL_CLIP    = 1 << 8;

    protected static final int ELEMENT_LAST_FLAG = FLAG_VERTICAL_CLIP;

    // Допустимая погрешность в координатах интерфейса
    public static final float EPSILON = 1e-2f;

    public int flags = FLAG_VISIBLE | FLAG_TOUCHABLE;

    public float x, y, width, height;
    public float minWidth, minHeight;
    public float prefWidth, prefHeight;
    public float maxWidth = Float.MAX_VALUE, maxHeight = Float.MAX_VALUE;

    public void set(float x, float y, float width, float height) {
        assert x >= 0;
        assert y >= 0;
        assert width >= 0;
        assert height >= 0;

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public @Nullable String id;

    public LayoutGroup<?> parent;
    public final ObjectArrayList<InputListener> inputListeners = new ObjectArrayList<>();

    public final Color color = Color.WHITE.copy();

    protected LayoutElement(@Nullable String id) {
        this.id = id;
    }

    protected static boolean equalsEps(float a, float b) {
        return MathUtil.equalsEps(a, b, EPSILON);
    }

    protected void setFlag(int flag, boolean st) {
        if (st) {
            this.flags |= flag;
        } else {
            this.flags &= ~flag;
        }
    }
    protected void flipFlag(int flag) { flags ^= flag; }
    protected boolean isFlag(int flag) { return (flags & flag) != 0; }

    public final This id(@Nullable String id) { this.id = id; return as(); }
    public final This setParent(@Nullable Group parent) { this.parent = (LayoutGroup<?>) parent; return as(); }

    public This color(int rgba8888) {
        color.setRgba8888(rgba8888);
        return as();
    }

    public This color(Color color) {
        this.color.set(color);
        return as();
    }

    public This minWidth(float v)   { minWidth = v; return as(); }
    public This minHeight(float v)  { minHeight = v; return as(); }
    public This prefWidth(float v)  { prefWidth = v; return as(); }
    public This prefHeight(float v) { prefHeight = v; return as(); }
    public final This setPosition(float x, float y) {
        // TODO проверки на изменение
        this.x = x;
        this.y = y;
        return as();
    }

    public This fixedSize(float width, float height) {
        minSize(width, height);
        prefSize(width, height);
        return as();
    }

    public This minSize(float width, float height) {
        this.minWidth = width;
        this.minHeight = height;
        return as();
    }

    public This prefSize(float width, float height) {
        this.prefWidth = width;
        this.prefHeight = height;
        return as();
    }

    // endregion

    // region Getters
    public final @Nullable String id() { return id; }

    /// @return `null` если элемент не добавлен в сцену. У всех активных элементов сцены есть родитель
    public final @Nullable LayoutGroup<?> parent() { return parent; }
    public final float x() { return x; }
    public final float y() { return y; }
    public final float width() { return width; }
    public final float height() { return height; }
    public final boolean visible() { return isFlag(FLAG_VISIBLE); }
    public final boolean fillParent() { return isFlag(FLAG_FILL_PARENT); }
    public final boolean horizontalClip() { return isFlag(FLAG_HORIZONTAL_CLIP); }
    public final boolean verticalClip() { return isFlag(FLAG_VERTICAL_CLIP); }

    public Color color() { return color; }

    public List<InputListener> listeners() { return inputListeners; }
    public List<LayoutElement<?>> children()  { return List.of(); }

    public float minWidth()   { return minWidth; }
    public float minHeight()  { return minHeight; }
    public float prefWidth()  { return prefWidth; }
    public float prefHeight() { return prefHeight; }
    public float maxWidth()   { return maxWidth; }
    public float maxHeight()  { return maxHeight; }
    // endregion

    public abstract void draw();

    protected void resize() {}
    protected void updateThis(float dt) {}

    public void update(float dt) {
        resize();
        flags &= ~(FLAG_X_CHANGED | FLAG_Y_CHANGED | FLAG_W_CHANGED | FLAG_H_CHANGED);
        updateThis(dt);
    }

    public final boolean remove() { return parent != null && parent.remove(this); }

    public final This addListener(InputListener listener) {
        inputListeners.add(listener);
        return as();
    }


    public final This setFillParent(boolean state) { setFlag(FLAG_FILL_PARENT, state); return as(); }
    public final This setVisible(boolean state) {
        setFlag(FLAG_VISIBLE, state);
        return as();
    }

    public final This setTouchable(boolean state) {
        setFlag(FLAG_TOUCHABLE, state);
        return as();
    }

    public final This setHotkey(int key, Runnable action) {
        addListener(new KeyboardListener(key, action));
        return as();
    }

    public final This setClickKey(int key, ClickType type, Runnable action) {
        addListener(new ClickListener(key, type) {
            protected void onPress(float x, float y) {
                action.run();
            }
        });
        return as();
    }

    public final This toggleVisibility() {
        flipFlag(FLAG_VISIBLE);
        return as();
    }

    public @Nullable LayoutElement<?> hit(float hx, float hy) {
        if ((flags & FLAG_TOUCHABLE) != 0 && Rectangle.contains(x, y, width, height, hx, hy)) {
            return as();
        }
        return null;
    }

    public String toString() {
        return getClass().getSimpleName() + "['" + id + "']";
    }

    // region InputListener


    @Override
    public void onFramebufferResize(int width, int height) {
        inputListeners.forEach(listener -> listener.onFramebufferResize(width, height));
    }

    @Override
    public void onTouchDown(float x, float y, int button) {
        inputListeners.forEach(listener -> listener.onTouchDown(x, y, button));
    }

    @Override
    public void onTouchUp(float x, float y, int button) {
        inputListeners.forEach(listener -> listener.onTouchUp(x, y, button));
    }

    @Override
    public void onScroll(float xOffset, float yOffset) {
        inputListeners.forEach(listener -> listener.onScroll(xOffset, yOffset));
    }

    @Override
    public void onMouseMove(float x, float y) {
        inputListeners.forEach(listener -> listener.onMouseMove(x, y));
    }

    @Override
    public void onMouseDragged(float x, float y) {
        inputListeners.forEach(listener -> listener.onMouseDragged(x, y));
    }

    @Override
    public void onKeyUp(int key, int scancode) {
        inputListeners.forEach(listener -> listener.onKeyUp(key, scancode));
    }

    @Override
    public void onKeyDown(int key, int scancode) {
        inputListeners.forEach(listener -> listener.onKeyDown(key, scancode));
    }

    @Override
    public void onKeyRepeat(int key, int scancode) {
        inputListeners.forEach(listener -> listener.onKeyRepeat(key, scancode));
    }

    @Override
    public void onCodepoint(int codepoint) {
        inputListeners.forEach(listener -> listener.onCodepoint(codepoint));
    }

    @Override
    public void onMouseEnter(float x, float y) {
        inputListeners.forEach(listener -> listener.onMouseEnter(x, y));
    }

    @Override
    public void onMouseExit(float x, float y) {
        inputListeners.forEach(listener -> listener.onMouseExit(x, y));
    }
    // endregion
}
