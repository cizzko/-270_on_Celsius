package core.GUI;

public class ButtonObject {
    public boolean visible, isClicked;
    public int x, y, width, height;
    public String name;

    public ButtonObject(boolean visible, int x, int y, int height, int width, String name) {
        this.isClicked = false;
        this.visible = visible;
        this.x = x;
        this.height = height;
        this.width = width;
        this.y = y;
        this.name = name;
    }
}