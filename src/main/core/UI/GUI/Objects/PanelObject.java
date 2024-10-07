package core.UI.GUI.Objects;

import core.Utils.SimpleColor;
import core.g2d.Atlas;


public class PanelObject extends Element {
    private int layer;
    private String name, group;
    private Atlas.Region texture;
    private boolean visible, simple;
    private SimpleColor color;

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

    public int getLayer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Atlas.Region getTexture() {
        return texture;
    }

    public void setTexture(Atlas.Region texture) {
        this.texture = texture;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isSimple() {
        return simple;
    }

    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    public SimpleColor getColor() {
        return color;
    }

    public void setColor(SimpleColor color) {
        this.color = color;
    }
}
