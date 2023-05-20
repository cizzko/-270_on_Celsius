package core.UI.GUI.objects;

import java.awt.*;

public class ButtonObject {
    public boolean visible, isClicked, simple, swapButton;
    public int x, y, width, height;
    public long lastClickTime;
    public Color color;
    public String name;

    public ButtonObject(boolean simple, boolean swapButton, boolean visible, int x, int y, int height, int width, String name, Color color) {
        this.isClicked = false;
        this.simple = simple;
        this.swapButton = swapButton;
        this.visible = visible;
        this.height = height;
        this.width = width;
        this.x = x;
        this.y = y;
        this.name = name;
        this.color = color;
        this.lastClickTime = System.currentTimeMillis();
    }
}