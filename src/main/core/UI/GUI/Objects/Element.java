package core.UI.GUI.Objects;

import core.math.Point2i;
import core.math.Rectangle;

public abstract class Element {

    public final int x, y;
    public final int width, height;

    public Element(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(Point2i point) {
        return Rectangle.contains(x, y, width, height, point);
    }
}
