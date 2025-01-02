package core.ui;

import core.Utils.Sized;
import core.math.Point2i;

public interface Element {
    // null если это корневой элемент интерфейса, т.е. специальная затычка
    // @Nullable
    Group parent();

    int x();

    int y();

    int width();

    int height();

    boolean visible();

    void draw();

    void update();

    Element setSize(int size);

    Element setSize(Sized sized);

    Element setSize(int width, int height);

    Element set(int x, int y, int width, int height);

    Element setX(int x);

    Element setY(int y);

    Element setPosition(int x, int y);

    Element setPosition(Point2i position);

    Element setVisible(boolean visible);

    Element toggleVisibility();

    Element hit(int x, int y);

    default Element hit(Point2i point) {
        return hit(point.x, point.y);
    }
}
