package core.ui;

import core.g2d.Fill;

import static core.Global.*;
import static core.World.Textures.TextureDrawing.drawPrompt;
import static core.World.Textures.TextureDrawing.drawText;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class ToggleButton extends BaseButton<ToggleButton> {
    protected ToggleButton(Group panel) {
        super(panel);
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
                clickAction.run();
            }
        }
    }

    @Override
    public void draw() {
        if (!visible) {
            return;
        }
        if (simple && isClicked) {
            Fill.rect(x, y, width, height, color);
            batch.draw(atlas.byPath("UI/GUI/checkMarkTrue"), x + width / 1.3f, y + height / 3f);
            drawText(x * 1.1f, y + height / 3f, name);
        } else if (simple) {
            Fill.rect(x, y, width, height, color);
            drawText(x * 1.1f, y + height / 3f, name);
        } else {
            Fill.rectangleBorder(x - 6, y - 6, width, height, 6, color);
            if (isClicked) {
                batch.draw(atlas.byPath("UI/GUI/checkMarkTrue"), x, y);
            } else {
                batch.draw(atlas.byPath("UI/GUI/checkMarkFalse"), x, y);
            }
            drawText(width + x + 24, y, name);
        }
        drawPrompt(this);
    }
}
