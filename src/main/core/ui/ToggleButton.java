package core.ui;

import core.Utils.SimpleColor;
import core.g2d.Drawable;
import core.g2d.Fill;

import static core.Global.*;
import static core.World.Textures.TextureDrawing.drawPrompt;
import static core.World.Textures.TextureDrawing.drawText;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class ToggleButton extends BaseButton<ToggleButton> {
    protected final Style.ToggleButton style;

    protected ToggleButton(Group panel, Style.ToggleButton style) {
        super(panel);
        this.style = style;
        setSize(style.width, style.height);
    }

    @Override
    public void update() {
        if (!visible) {
            return;
        }
        if (!isClickable) {
            return;
        }
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
        if (!visible) {
            return;
        }
        float offset = style.borderOffset;
        SimpleColor c = color;
        if (c == null) c = style.backgroundColor;

        Fill.rectangleBorder(x - offset, y - offset, width, height, offset, c);

        Drawable tex = isClicked ? style.checkUp : style.checkDown;
        batch.draw(tex, x, y);
        drawText(width + x + style.textOffset, y, name);

        drawPrompt(this);
    }
}
