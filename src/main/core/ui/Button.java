package core.ui;

import core.g2d.Fill;

import static core.Global.input;
import static core.World.Textures.TextureDrawing.drawText;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class Button extends BaseButton<Button> {
    protected final Style.TextButton style;

    protected Button(Group panel, Style.TextButton style) {
        super(panel);
        this.style = style;
    }

    @Override
    public void updateThis() {
        if (!visible()) {
            return;
        }
        if (!isClickable) {
            return;
        }
        boolean press = hit(input.mousePos()) == this && input.justClicked(GLFW_MOUSE_BUTTON_1);
        isClicked = press;
        if (press && clickAction != null) {
            clickAction.accept(this);
        }

        if (isClicked && oneShot) {
            isClickable = false;
        }
    }

    @Override
    public void draw() {
        if (!visible()) {
            return;
        }

        var backgroundColor = color;
        if (backgroundColor == null) backgroundColor = style.backgroundColor;

        float borderWidth = style.borderWidth;
        if (borderWidth == 0) {
            Fill.rect(x, y, width, height, backgroundColor);
        } else {
            Fill.rectangleBorder(x, y, width, height, borderWidth, backgroundColor);
        }

        var disabledColor = style.disabledColor;
        if (disabledColor != null) {
            if (isClicked) {
                Fill.rect(x, y, width, height, disabledColor);
            }
        }
        if (name != null) {
            drawText(x + 20, y + height / 2.8f, name);
        }
        drawPrompt(this, style.font);
    }
}
