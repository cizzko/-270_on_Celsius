package core.ui.GUI.Objects;

import core.Utils.SimpleColor;
import core.g2d.Atlas;


public class PanelObject extends Element {
    public int layer;
    public String name, group;
    public Atlas.Region texture;
    public boolean visible, simple;
    public SimpleColor color;

    public PanelObject(int x, int y, int width, int height, int layer, String name, boolean simple, Atlas.Region texture, String group, SimpleColor color) {
        super(x, y, width, height);
        if (group == null) {
            group = "None";
        }

        this.color = color;
        this.layer = layer;
        this.texture = texture;
        this.visible = true;
        this.simple = simple;
        this.group = group;
        this.name = name;
    }
}
