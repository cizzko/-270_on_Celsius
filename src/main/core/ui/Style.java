package core.ui;

import core.Utils.Color;
import core.g2d.Drawable;
import core.g2d.Font;

public abstract class Style {

    public abstract void load();

    public static abstract class Panel extends Style {
        public float borderWidth;
        public Color backgroundColor;
    }

    public static abstract class TextButton extends Style {
        public float borderWidth;
        public Color disabledColor;
        public Color backgroundColor;
    }

    public static abstract class ToggleButton extends Style {
        public float width, height;
        public Drawable checkUp, checkDown;
        public Color backgroundColor;
        public float borderOffset;
        public float textOffset;
    }

    public static abstract class Text extends Style {
        public Color color;
        public Font font;
    }
}
