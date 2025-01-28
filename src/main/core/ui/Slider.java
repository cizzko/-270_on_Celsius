package core.ui;

import core.Utils.Color;
import core.Window;
import core.g2d.Atlas;
import core.g2d.Fill;
import core.g2d.Font;

import static core.Global.*;
import static core.World.Textures.TextureDrawing.getTextSize;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class Slider extends BaseElement<Slider> {
    private float sliderPos, prevSliderPos;
    private boolean isClicked;

    public int min, max;
    public Color sliderColor, dotColor;
    public MoveListener updater;

    public interface MoveListener {
        void update(int pos, int max);
    }

    protected Slider(Group parent) {
        super(parent);
    }

    public int getSliderValue() {
        float relativePos = (sliderPos - x) / width;
        return Math.round(relativePos * (max - min) + min);
    }

    public Slider onMove(MoveListener updater) {
        this.updater = updater;
        return this;
    }

    public Slider setBounds(int min, int max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public Slider setSliderColor(Color color) {
        this.sliderColor = color;
        return this;
    }

    public Slider setDotColor(Color color) {
        this.dotColor = color;
        return this;
    }

    @Override
    public void draw() {
        if (!visible()) {
            return;
        }

        Fill.rect(x, y, sliderPos - x, height, sliderColor);

        int oa = sliderColor.a();
        sliderColor.a(oa - 100);
        Fill.rect(x, y, width, height, sliderColor);
        sliderColor.a(oa); // А что? Жава не может без выделения памяти в куче

        int rectHeight = 30;
        int rectBrightness = 170;
        int rectY = 45;
        float rectWidth = 1.75f;
        boolean notEqual = Math.abs(prevSliderPos - sliderPos) > 0.001f;
        if (!notEqual) {
            rectHeight = 26;
            rectWidth = 2.5f;
            rectY = 40;
            rectBrightness = 120;
        }

        Atlas.Region triangle = atlas.byPath("UI/GUI/numberBoardTriangle.png");

        // todo 7 это высота текстуры треугольника

        batch.draw(triangle, sliderPos - (triangle.width() / 2f), y + rectY - 7);

        String sliderValue = Integer.toString(getSliderValue());
        int numbersWidth = getTextSize(sliderValue).width;

        Fill.rect(sliderPos - (triangle.width() / 2f) - (numbersWidth / (rectWidth * 2)),
                y + rectY, 30 + numbersWidth / rectWidth, rectHeight,
                Color.fromRgba8888(0, 0, 0, rectBrightness));

        float x = sliderPos - (numbersWidth / 2f) + 5;
        for (int i = 0; i < sliderValue.length(); i++) {
            char ch = sliderValue.charAt(i);
            if (ch == ' ') {
                x += Window.defaultFont.getGlyph('A').width();
                continue;
            }
            Font.Glyph glyph = Window.defaultFont.getGlyph(ch);
            batch.draw(glyph, Styles.DIRTY_WHITE, x, y + rectY);
            x += glyph.width();
        }

        Fill.circle(sliderPos - 0.875f*height, y - 5, height * 1.75f, dotColor);
    }

    @Override
    protected void resize() {
        if ((flags & FLAG_X_CHANGED) != 0) {
            this.prevSliderPos = this.sliderPos = x + 1;
        }
    }

    @Override
    public void updateThis() {
        boolean hit = hit(input.mousePos()) == this;
        if (hit) {
            if (input.justClicked(GLFW_MOUSE_BUTTON_1)) {
                isClicked = true;
            }
            boolean wasClicked = isClicked;
            if (wasClicked && input.clicked(GLFW_MOUSE_BUTTON_1)) {
                prevSliderPos = sliderPos;
                sliderPos = input.mousePos().x;
                if (updater != null) {
                    updater.update(getSliderValue(), max);
                }
            }
        } else {
            isClicked = false;
        }
    }
}
