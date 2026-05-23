package core.UI;

import core.g2d.Drawable;
import core.g2d.StackfulRender;
import core.graphic.Color;

public class ImageElement extends BaseElement<ImageElement> {
    private int color = Color.white;

    public Drawable image;

    protected ImageElement(Group parent) {
        super(parent);
    }


    public ImageElement setImage(Drawable image) {
        this.image = image;
        setSize(image.width(), image.height());
        return this;
    }

    public ImageElement setColor(Color color) { this.color = color.rgba8888(); return this; }

    @Override
    public void draw() {
        if (image != null) {
            StackfulRender.draw(image, color, x, y, width, height);
        }
    }
}
