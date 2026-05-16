package core.UI;

import core.input.InputListener;
import core.math.Point2i;
import core.math.Vector2f;
import core.util.Sized;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface Element extends InputListener {
    @Nullable String id();

    /// @return `null` если элемент не добавлен в сцену. У всех элементов сцены есть родитель
    @Nullable Group parent();

    float x();
    float y();

    float width();
    float height();

    boolean visible();

    void draw();

    void update(float dt);
    boolean remove();

    Element setId(@Nullable String id);
    Element setParent(@Nullable Group parent);

    Element setX(float x);
    Element setY(float y);
    Element setWidth(float width);
    Element setHeight(float height);

    Element setPosition(float x, float y);
    Element setSize(Sized sized);
    Element setSize(float width, float height);
    Element set(float x, float y, float width, float height);

    Element setVisible(boolean state);
    Element toggleVisibility();

    Element setTouchable(boolean state);

    Element setHotkey(int key, Runnable action);

    @Nullable Element hit(float x, float y);
    default @Nullable Element hit(Point2i point) { return hit(point.x, point.y); }
    default @Nullable Element hit(Vector2f point) { return hit(point.x, point.y); }

    @SuppressWarnings("unchecked")
    default <E extends Element> E as() { return (E) this; }

    default boolean isDescendantOf(Predicate<Element> pred) {
        Element parent = this;
        while (parent != null) {
            if (pred.test(parent)) {
                return true;
            }
            parent = parent.parent();
        }
        return false;
    }
}
