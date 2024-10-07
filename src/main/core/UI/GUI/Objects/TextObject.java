package core.UI.GUI.Objects;

import core.Utils.SimpleColor;

public class TextObject {
    private String text, group;
    private float x, y;
    private SimpleColor color;
    private boolean visible;

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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public SimpleColor getColor() {
        return color;
    }

    public void setColor(SimpleColor color) {
        this.color = color;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
