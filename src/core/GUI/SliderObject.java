package core.GUI;

public class SliderObject {
    public int x, y, width, height, sliderPos, max;
    public boolean isClicked, visible;

    public SliderObject(boolean visible, int x, int y, int width, int height, int max) {
        this.x = x;
        this.visible = visible;
        this.isClicked = false;
        this.sliderPos = 0;
        this.y = y;
        this.max = max;
        this.width = width;
        this.height = height;
    }
}
