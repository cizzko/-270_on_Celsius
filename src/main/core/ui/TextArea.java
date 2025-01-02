package core.ui;

import core.Utils.SimpleColor;

import static core.World.Textures.TextureDrawing.drawText;

public class TextArea extends BaseElement<TextArea> {
    public String text;
    public SimpleColor color;

    public TextArea() {
        this(null);
    }

    protected TextArea(Group parent) {
        super(parent);
    }

    public TextArea setText(String text) {
        this.text = text;
        return this;
    }

    public TextArea setColor(SimpleColor color) {
        this.color = color;
        return this;
    }

    @Override
    public void draw() {
        if (!visible) {
            return;
        }
        if (text != null) {
            drawText(x, y, text, color);
        }
    }

    @Override
    public void update() {
    }
}
