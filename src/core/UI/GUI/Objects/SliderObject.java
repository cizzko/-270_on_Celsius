package core.UI.GUI.Objects;

import core.World.Textures.SimpleColor;

public class SliderObject {
    public int x, y, width, height, sliderPos, max;
    public boolean isClicked, visible;
    public SimpleColor sliderColor, dotColor;

    public SliderObject(int x, int y, int width, int height, int max, SimpleColor sliderColor, SimpleColor dotColor) {
        this.x = x;
        this.y = y;
        this.sliderColor = sliderColor;
        this.dotColor = dotColor;
        this.visible = true;
        this.isClicked = false;
        this.sliderPos = 0;
        this.max = max;
        this.width = width;
        this.height = height;
    }
}
