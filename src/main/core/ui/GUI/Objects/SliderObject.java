package core.ui.GUI.Objects;

import core.Utils.SimpleColor;

public class SliderObject extends Element {
    public int sliderPos, max;
    public boolean isClicked, visible;
    public SimpleColor sliderColor, dotColor;

    public SliderObject(int x, int y, int width, int height, int max, SimpleColor sliderColor, SimpleColor dotColor) {
        super(x, y, width, height);
        this.sliderColor = sliderColor;
        this.dotColor = dotColor;
        this.visible = true;
        this.isClicked = false;
        this.sliderPos = 0;
        this.max = max;
    }
}
