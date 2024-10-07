package core.UI.GUI.Objects;

import core.Utils.SimpleColor;

public class SliderObject extends Element {
    private int sliderPos, lastSliderPos, max;
    private boolean isClicked, visible;
    private SimpleColor sliderColor, dotColor;

    public SliderObject(int x, int y, int width, int height, int max, SimpleColor sliderColor, SimpleColor dotColor) {
        super(x, y, width, height);
        this.sliderColor = sliderColor;
        this.dotColor = dotColor;
        this.visible = true;
        this.isClicked = false;
        this.sliderPos = 0;
        this.lastSliderPos = 0;
        this.max = max;
    }

    public int getSliderPos() {
        return sliderPos;
    }

    public void setSliderPos(int sliderPos) {
        this.sliderPos = sliderPos;
    }

    public int getLastSliderPos() {
        return lastSliderPos;
    }

    public void setLastSliderPos(int lastSliderPos) {
        this.lastSliderPos = lastSliderPos;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public boolean isClicked() {
        return isClicked;
    }

    public void setClicked(boolean clicked) {
        isClicked = clicked;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public SimpleColor getSliderColor() {
        return sliderColor;
    }

    public void setSliderColor(SimpleColor sliderColor) {
        this.sliderColor = sliderColor;
    }

    public SimpleColor getDotColor() {
        return dotColor;
    }

    public void setDotColor(SimpleColor dotColor) {
        this.dotColor = dotColor;
    }

    public int getSliderValue() {
        float relativePos = (float) (sliderPos - x) / width;
        return Math.round(relativePos * max);
    }

    public int getLastSliderValue() {
        float relativePos = (float) (lastSliderPos - x) / width;
        return Math.round(relativePos * max);
    }
}
