package core.ui;

import core.Utils.Sized;
import core.math.Point2i;

public interface Element {
    String id();

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

    Element hit(float x, float y);

    default Element hit(Point2i point) {
        return hit(point.x, point.y);
    }
}
