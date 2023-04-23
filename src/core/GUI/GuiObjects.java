package core.GUI;

public class GuiObjects extends Thread {
    public boolean visible, isClicked, isPanel, isButton, isSlider;
    public int x, y, min, max;
    public String name, path;

    public GuiObjects(boolean isClicked, boolean visible, int x, int y, String name, String path) {
        this.isClicked = isClicked;
        this.isPanel = false;
        this.isButton = false;
        this.isSlider = false;
        this.visible = visible;
        this.x = x;
        this.min = 0;
        this.max = 0;
        this.y = y;
        this.name = name;
        this.path = path;
    }
}