package core.UI.GUI.Objects;

import java.awt.*;

public class ButtonObject {
    public boolean visible, isClicked, simple, swapButton, isClickable;
    public int x, y, width, height;
    public long lastClickTime;
    public Color color;
    public String name;

    public ButtonObject(boolean simple, boolean swapButton, int x, int y, int height, int width, String name, Color color) {
        this.isClicked = false;
        this.isClickable = true;
        this.simple = simple;
        this.swapButton = swapButton;
        this.visible = true;
        this.height = height;
        this.width = width;
        this.x = x;
        this.y = y;
        this.name = name;
        this.color = color;
        this.lastClickTime = System.currentTimeMillis();
    }
}