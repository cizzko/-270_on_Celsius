package core.UI;

import core.input.InputListener;
import core.math.Point2i;
import core.util.Color;
import core.util.Sized;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface Element extends InputListener {
    String id();

    // null если это корневой элемент интерфейса, т.е. специальная затычка
    @Nullable Group parent();

    float x();

    float y();

    float width();

    float height();

    boolean visible();

    void draw();

    void update(float dt);
    boolean remove();

    Element setId(String id);

    Element setX(float x);
    Element setY(float y);
    Element setWidth(float width);
    Element setHeight(float height);

    Element setPosition(float x, float y);
    Element setSize(Sized sized);
    Element setSize(float width, float height);
    Element set(float x, float y, float width, float height);

    Element setVisible(boolean visible);

    Element toggleVisibility();

    @Nullable Element hit(float x, float y);
    default @Nullable Element hit(Point2i point) {
        return hit(point.x, point.y);
    }

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

    default Color color() {
        // TODO плохой дизайн интерфейса
        return null;
    }
}
