package core.GUI.objects;

public class PanelObject {
    public int x, y, width, height;
    public String name;
    public boolean visible, simple;

    public PanelObject(int x, int y, int width, int height, String name, boolean visible, boolean simple) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.visible = visible;
        this.simple = simple;
        this.name = name;
    }
}
