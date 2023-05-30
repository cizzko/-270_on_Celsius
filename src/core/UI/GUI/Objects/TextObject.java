package core.UI.GUI.Objects;

import java.awt.Color;

public class TextObject {
    public String text;
    public int x, y;
    public Color color;
    public boolean visible;

    public TextObject(int x, int y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.visible = true;
    }
}
