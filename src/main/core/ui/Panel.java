package core.ui;

import core.Utils.SimpleColor;
import core.g2d.Fill;

public class Panel extends BaseGroup<Panel> {
    public boolean simple;
    public SimpleColor color = Styles.DEFAULT_PANEL_COLOR;

    public Panel(Group parent) {
        super(parent);
    }

    public Panel setSimple(boolean simple) {
        this.simple = simple;
        return this;
    }

    public Panel setColor(SimpleColor color) {
        this.color = color;
        return this;
    }

    @Override
    public void draw() {
        if (!visible) {
            return;
        }
        if (!simple) {
            Fill.rect(x, y, width, height, color);
            Fill.rectangleBorder(x, y, width, height, 20, color);
        } else {
            Fill.rect(x, y, width, height, color);
        }
        if (children != null) {
            for (Element child : children) {
                child.draw();
            }
        }
    }
}
