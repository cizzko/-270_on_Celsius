package core.ui;

import core.util.Color;
import core.g2d.Fill;

public class Panel extends BaseGroup<Panel> {
    public final Style.Panel style;

    public Color color = Styles.DEFAULT_PANEL_COLOR;

    public Panel(Group parent, Style.Panel style) {
        super(parent);
        this.style = style;
    }

    public Panel setColor(Color color) {
        this.color = color;
        return this;
    }

    @Override
    public void drawThis() {
        var backgroundColor = color;
        if (backgroundColor == null) backgroundColor = style.backgroundColor;

        Fill.rect(x, y, width, height, backgroundColor);

        float borderWidth = style.borderWidth;
        if (borderWidth != 0) {
            Fill.rectangleBorder(x, y, width, height, borderWidth, backgroundColor);
        }
    }

    // Метод для настройки вкладок. То есть это некоторый список действий (у нас за это кнопки отвечают),
    // в котором при на выполнении одного действия, нужно потом выполнить другое, чтобы разблокировать вновь это действие
    // Мне это нужно для красивого оформления, которое даёт setClickable(false)
    public void oneOf(Button... buttons) {
        for (Button button : buttons) {
            var oldAction = button.clickAction;
            button.onClick(b -> {
                b.setClickable(false);
                for (Button other : buttons) {
                    if (other != b) {
                        other.setClickable(true);
                    }
                }
                oldAction.accept(b);
            });
        }
    }
}
