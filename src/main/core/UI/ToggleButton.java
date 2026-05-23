package core.UI;

import core.g2d.Drawable;
import core.g2d.StackfulRender;
import core.g2d.Fill;
import core.graphic.Color;
import org.jetbrains.annotations.Nullable;

import static core.Global.input;
import static core.graphic.GuiDrawing.drawText;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class ToggleButton extends BaseButton<ToggleButton> {
    protected final Style.ToggleButton style;

    public @Nullable Drawable customCheckUp;
    public boolean checkboxAllText;

    protected ToggleButton(Group panel, Style.ToggleButton style) {
        super(panel);
        this.style = style;
        setSize(style.width, style.height);
    }

    public ToggleButton setCustomCheckUp(@Nullable Drawable checkUp) {
        this.customCheckUp = checkUp;
        return this;
    }

    public ToggleButton setCheckboxAllText(boolean checkboxAllText) {
        this.checkboxAllText = checkboxAllText;
        return this;
    }

    @Override
    protected void resize() {
        name.set(width + x + style.textOffset, y, 0, 0);
        name.resize();
    }

    @Override
    public void updateThis(float dt) {
        if (!visible()) {
            return;
        }
        if (!isClickable) {
            return;
        }
        name.update(dt);
        if (hit(input.mousePos()) == this && input.justClicked(GLFW_MOUSE_BUTTON_1)) {
            isClicked = !isClicked;
            if (clickAction != null) {
                clickAction.accept(this);
            }
        }
        if (isClicked && oneShot) {
            isClickable = false;
        }
    }

    @Override
    public void draw() {
        float margin = style.borderOffset;
        Color c = color;
        if (c == null) {
            c = style.backgroundColor;
        }

        var checkUp = customCheckUp;
        if (checkUp == null) {
            checkUp = style.checkUp;
        }

        Fill.rectangleBorder(x - margin, y - margin, width + margin*2, height + margin*2, margin, c.rgba8888());

        Drawable tex = isClicked ? checkUp : style.checkDown;
        StackfulRender.draw(tex, x, y, width, height);

        name.draw();

        // drawPrompt(this, style.font);
    }
}
