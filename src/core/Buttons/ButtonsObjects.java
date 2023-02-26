package core.Buttons;

public class ButtonsObjects extends Thread {
    public boolean visible, isClicked;
    public int x, y;
    public String name, path;

    public ButtonsObjects(boolean isClicked, boolean visible, int x, int y, String name, String path) {
        this.isClicked = isClicked;
        this.visible = visible;
        this.x = x;
        this.y = y;
        this.name = name;
        this.path = path;
    }
}