package core.ui.widget;

import core.ui.Style;
import core.g2d.Fill;
import core.ui.Table;
import org.jetbrains.annotations.Nullable;

public sealed class Panel extends Table
        permits DockPanel, DropDownMenu, Console {

    public float borderWidth;

    public Panel(@Nullable String id, Style.Panel style) {
        super(id);
        this.color.set(style.backgroundColor);
        this.borderWidth = style.borderWidth;
        pad(borderWidth);
    }

    @Override
    protected void drawThis() {
        Fill.rect(x, y, width, height, color);
        float bwidth = borderWidth;
        if (bwidth > 0)
            Fill.rectangleBorder(x, y, width, height, bwidth, color.rgba8888());
    }
}
