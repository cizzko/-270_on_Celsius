package core.ui;

import core.g2d.Drawable;
import core.g2d.Font;
import core.graphic.Color;

public class Style {

    public void load() {}

    public static abstract class Panel extends Style {
        public float borderWidth;
        public Color backgroundColor;
    }

    public static abstract class Button extends Style {
        public Font font;
        public Color backgroundColor;

        // TODO
        public Text text;
    }

    public static abstract class TextButton extends Button {
        public float borderWidth;
        public Color disabledColor;
        public float minWidth;
        public float prefHeight;
    }

    public static abstract class ToggleButton extends Button {
        public float width, height;
        public Drawable checkUp, checkDown;
        public float borderOffset;
        public float textOffset;
    }

    public static abstract class Text extends Style {
        public Color color;
        public Font font;
    }

    public static abstract class TextField extends Style {
        public Font font;
        public Drawable background;
        public Color textColor;
    }

    public static abstract class Slider extends Style {
        public Color sliderColor, dotColor;
        public Font font;
    }
}
