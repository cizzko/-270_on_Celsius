package core.ui;

import core.Global;
import core.g2d.Drawable;

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
        if (!visible) {
            return;
        }
        if (image != null) {
            Global.batch.draw(image, x, y);
        }
    }

    @Override
    public void update() {
    }
}
