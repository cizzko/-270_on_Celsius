package core.ui;

import core.Utils.SimpleColor;
import core.g2d.Fill;

import static core.World.Textures.TextureDrawing.drawPrompt;
import static core.World.Textures.TextureDrawing.drawText;

public class Button extends BaseButton<Button> {
    protected Button(Group panel) {
        super(panel);
    }

    @Override
    public void draw() {
        if (!visible) {
            return;
        }
        if (simple) {
            Fill.rect(x, y, width, height, color);
        } else {
            Fill.rectangleBorder(x, y, width, height, 6, color);
        }
        if (!isClickable) {
            Fill.rect(x, y, width, height, SimpleColor.fromRGBA(0, 0, 0, 123));
        }
        if (name != null) {
            drawText(x + 20, y + height / 2.8f, name);
        }
        drawPrompt(this);
    }
}
