package core.ui;

import core.Utils.SimpleColor;
import core.g2d.Drawable;

public abstract class Style {

    public abstract void load();

    public static abstract class Panel extends Style {
        public float borderWidth;
        public SimpleColor backgroundColor;
    }

    public static abstract class TextButton extends Style {
        public float borderWidth;
        public SimpleColor disabledColor;
        public SimpleColor backgroundColor;
    }

    public static abstract class ToggleButton extends Style {
        public float width, height;
        public Drawable checkUp, checkDown;
        public SimpleColor backgroundColor;
        public float borderOffset;
        public float textOffset;
    }
}
