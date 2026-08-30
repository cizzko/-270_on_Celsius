package core.ui.widget;

import core.g2d.Atlas;
import core.g2d.Fill;
import core.g2d.Font;
import core.g2d.StackfulRender;
import core.math.TmpShapes;
import core.ui.*;
import org.jetbrains.annotations.Nullable;

import static core.Global.atlas;
import static core.graphic.Color.rgba8888;
import static core.graphic.GuiDrawing.calculateTextSize;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class Slider extends LayoutElement<Slider> {
    private static final int FLAG_ZERO      = ELEMENT_LAST_FLAG << 1;
    private static final int FLAG_CLICKABLE = FLAG_ZERO << 1;

    public final Style.Slider style;

    private float sliderPos;
    private int sliderValue, lastSliderValue;

    public int min, max;
    public MoveListener updater;

    public interface MoveListener {
        void update(int pos, int max);
    }

    public Slider(@Nullable String id, Style.Slider style) {
        super(id);
        this.style = style;

        setFlag(FLAG_CLICKABLE, true);

        addListener(new ClickListener(GLFW_MOUSE_BUTTON_1, ClickType.PRESS) {
            @Override
            protected void onDrag(float mx, float my) {
                if (!isFlag(FLAG_CLICKABLE)) {
                    return;
                }

                lastSliderValue = sliderValue;
                sliderPos = Math.clamp(mx, x, x+width);
                float norm = (sliderPos - x) / width;
                sliderValue = Math.round(norm * (max - min) + min);

                updater.update(sliderValue, max);
            }
        });
    }

    public void setClickable(boolean state) {
        setFlag(FLAG_CLICKABLE, state);
    }

    public Slider onMove(MoveListener updater) {
        this.updater = updater;
        return this;
    }

    public Slider bounds(int min, int max) {
        // assert min <= max;
        this.sliderValue = min;
        this.min = min;
        this.max = max;
        return this;
    }

    @Override
    public void onLayoutComplete() {
        if (min == max) {
            return;
        }

        float norm = (float)(sliderValue - min) / (max - min);
        norm = Math.clamp(norm, 0, 1);
        sliderPos = x + (norm * width);
    }

    @Override
    public void draw() {
        float sliderX = sliderPos;

        final float radius = height * 1.75f;
        Fill.rect(x, y, sliderX - x, height, style.sliderColor);

        var tmp = TmpShapes.c1;
        tmp.set(style.sliderColor);
        tmp.a(tmp.a() - 100);
        Fill.rect(x, y, width, height, tmp);

        int rectHeight = 30;
        int rectBrightness = 170;
        int rectY = 45;
        float rectWidth = 1.75f;
        if (lastSliderValue != sliderValue) {
            rectHeight = 26;
            rectWidth = 2.5f;
            rectY = 40;
            rectBrightness = 120;
        }

        Atlas.Region triangle = atlas.get("UI/GUI/numberBoardTriangle");
        StackfulRender.draw(triangle, sliderX - (triangle.width() / 2f), y + rectY - triangle.height());

        String sliderValueStr = Integer.toString(sliderValue);
        int numbersWidth = calculateTextSize(sliderValueStr).x;

        Fill.rect(sliderX - (triangle.width() / 2f) - (numbersWidth / (rectWidth * 2)),
                y + rectY, 30 + numbersWidth / rectWidth, rectHeight,
                rgba8888(0, 0, 0, rectBrightness));

        float x = sliderX - (numbersWidth / 2f) + 5;
        for (int i = 0; i < sliderValueStr.length(); i++) { // TODO заменить
            char ch = sliderValueStr.charAt(i);
            Font.Glyph glyph = style.font.getGlyph(ch);
            StackfulRender.draw(glyph, Styles.DIRTY_WHITE, x, y + rectY);
            x += glyph.width();
        }

        Fill.circle(sliderX - 0.875f * height, y - 5, radius, style.dotColor);
    }
}
