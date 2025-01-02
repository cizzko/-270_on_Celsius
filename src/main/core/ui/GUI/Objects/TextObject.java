package core.ui.GUI.Objects;

import core.Utils.SimpleColor;

public class TextObject {
    public String text, group;
    public float x, y;
    public SimpleColor color;
    public boolean visible;

    public TextObject(float x, float y, String text, SimpleColor color, String group) {
        if (group == null) {
            group = "None";
        }
        this.x = x;
        this.y = y;
        this.text = text;
        this.group = group;
        this.color = color;
        this.visible = true;
    }
}
