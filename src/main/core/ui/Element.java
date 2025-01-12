package core.ui;

import core.Utils.Sized;
import core.math.Point2i;

public interface Element {
    // null если это корневой элемент интерфейса, т.е. специальная затычка
    // @Nullable
    Group parent();

    float x();

    float y();

    float width();

    float height();

    boolean visible();

    void draw();

    void update();

    Element setSize(float size);

    Element setSize(Sized sized);

    Element setSize(float width, float height);

    Element set(float x, float y, float width, float height);

    Element setX(float x);

    Element setY(float y);

    Element setPosition(float x, float y);

    Element setPosition(Point2i position);

    Element setVisible(boolean visible);

    Element toggleVisibility();

    Element hit(float x, float y);

    default Element hit(Point2i point) {
        return hit(point.x, point.y);
    }
}
