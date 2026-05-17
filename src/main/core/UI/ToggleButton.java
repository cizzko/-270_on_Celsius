package core.UI;

import core.g2d.Drawable;
import core.g2d.StackfulRender;
import core.g2d.Fill;
import core.util.Color;

import static core.Global.input;
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

        Fill.rectangleBorder(x - margin, y - margin, width + margin*2, height + margin*2, margin, c.rgba8888());

        Drawable tex = isClicked ? style.checkUp : style.checkDown;
        StackfulRender.draw(tex, x, y, width, height);

        name.draw();

        // drawPrompt(this, style.font);
    }
}
