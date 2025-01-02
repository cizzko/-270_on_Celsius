package core.ui;

import core.Utils.SimpleColor;
import core.g2d.Fill;

import static core.Global.batch;
import static core.Global.input;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class Slider extends BaseElement<Slider> {
    private int sliderPos;
    private boolean isClicked;
    public int max;
    public SimpleColor sliderColor, dotColor;
    public MoveListener updater;

    public interface MoveListener {
        void update(int pos, int max);
    }

    protected Slider(Group parent) {
        super(parent);
    }

    public int getSliderPos() {
        float relativePos = (float) (sliderPos - x) / width;
        return Math.round(relativePos * max);
    }

    public Slider onMove(MoveListener updater) {
        this.updater = updater;
        return this;
    }

    public Slider setMax(int max) {
        this.max = max;
        return this;
    }

    public Slider setSliderColor(SimpleColor color) {
        this.sliderColor = color;
        return this;
    }

    public Slider setDotColor(SimpleColor color) {
        this.dotColor = color;
        return this;
    }

    @Override
    public void draw() {
        if (!visible) {
            return;
        }
        Fill.rect(x, y, width, height, sliderColor);
        Fill.circle(sliderPos, y - 5, height * 1.5f, dotColor);
    }

    @Override
    public void update() {
        boolean hit = hit(input.mousePos()) == this;
        if (hit) {
            if (input.justClicked(GLFW_MOUSE_BUTTON_1)) {
                isClicked = true;
            }
            boolean wasClicked = isClicked;
            if (wasClicked && input.clicked(GLFW_MOUSE_BUTTON_1)) {
                sliderPos = input.mousePos().x;
                if (updater != null) {
                    updater.update(getSliderPos(), max);
                }
            }
        } else {
            isClicked = false;
        }
    }

    @Override
    public Slider setX(int x) {
        this.sliderPos = x + 1;
        return super.setX(x);
    }
}
