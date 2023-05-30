package core.UI.GUI.Objects;

import java.awt.*;

public class SliderObject {
    public int x, y, width, height, sliderPos, max;
    public boolean isClicked, visible;
    public Color sliderColor, dotColor;

    public SliderObject(int x, int y, int width, int height, int max, Color sliderColor, Color dotColor) {
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
