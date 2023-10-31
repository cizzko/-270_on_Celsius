package core.UI.GUI.Objects;

import core.Utils.SimpleColor;

public class TextObject {
    public String text, group;
    public int x, y;
    public SimpleColor color;
    public boolean visible;

    public TextObject(int x, int y, String text, SimpleColor color, String group) {
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
