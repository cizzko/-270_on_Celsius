package core.ui;

import core.EventHandling.EventHandler;
import core.Global;
import core.g2d.Drawable;

public class ImageButton extends BaseElement<ImageButton> {
    public boolean isClickable = true, isClicked;
    public Runnable clickAction;
    public Drawable image;

    protected ImageButton(Group parent) {
        super(parent);
    }

    public ImageButton setImage(Drawable image) {
        this.image = image;
        setSize(image);
        return this;
    }

    public ImageButton onClick(Runnable clickAction) {
        this.clickAction = clickAction;
        return this;
    }

    public ImageButton setClickable(boolean isClickable) {
        this.isClickable = isClickable;
        return this;
    }

    @Override
    public void updateThis() {
        if (!isClickable) {
            return;
        }
        boolean press = EventHandler.isMousePressed(this);
        isClicked = press;
        if (press && clickAction != null) {
            clickAction.run();
        }
    }

    @Override
    public void draw() {
        if (image != null) {
            Global.batch.draw(image, x, y);
        }
    }
}
