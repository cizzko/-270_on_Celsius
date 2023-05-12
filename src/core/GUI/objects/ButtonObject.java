package core.GUI.objects;

import java.awt.*;

public class ButtonObject {
    public boolean visible, isClicked;
    public int x, y, width, height;
    public Color color;
    public String name;

    public ButtonObject(boolean visible, int x, int y, int height, int width, String name, Color color) {
        this.isClicked = false;
        this.visible = visible;
        this.height = height;
        this.width = width;
        this.x = x;
        this.y = y;
        this.name = name;
        this.color = color;
    }
}