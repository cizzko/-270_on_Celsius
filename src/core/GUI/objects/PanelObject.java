package core.GUI.objects;

public class PanelObject {
    public int x, y, width, height, layer;
    public String name, options;
    public boolean visible, simple;

    public PanelObject(int x, int y, int width, int height, int layer, String name, boolean visible, boolean simple, String options) {
        this.x = x;
        this.y = y;
        this.layer = layer;
        this.width = width;
        this.height = height;
        this.options = options;
        this.visible = visible;
        this.simple = simple;
        this.name = name;
    }
}
