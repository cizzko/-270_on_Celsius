package core.UI;

import core.g2d.Drawable;
import core.g2d.StackfulRender;

public class ImageElement extends BaseElement<ImageElement> {
    public Drawable image;

    protected ImageElement(Group parent) {
        super(parent);
    }

    public ImageElement setImage(Drawable image) {
        this.image = image;
        setSize(image.width(), image.height());
        return this;
    }

    @Override
    public void draw() {
        if (image != null) {
            StackfulRender.draw(image, x, y);
        }
    }
}
