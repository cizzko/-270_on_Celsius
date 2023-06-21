package core.UI.GUI.Objects;

import java.awt.*;

public class PanelObject {
    public int x, y, width, height, layer;
    public String name, options, group;
    public boolean visible, simple;
    public Color color;

    public PanelObject(int x, int y, int width, int height, int layer, String name, boolean simple, String options, String group, Color color) {
        if (group == null) {
            group = "None";
        }

        this.x = x;
        this.y = y;
        this.color = color;
        this.layer = layer;
        this.width = width;
        this.height = height;
        this.options = options;
        this.visible = true;
        this.simple = simple;
        this.group = group;
        this.name = name;
    }
}
