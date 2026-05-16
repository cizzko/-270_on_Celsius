package core.UI;

import core.EventHandling.EventHandler;
import core.Global;
import core.g2d.Drawable;
import core.g2d.Fill;
import core.util.Color;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static core.util.Color.red;

public class ImageButton extends BaseElement<ImageButton> {
    public Runnable clickAction;
    public Drawable image;

    protected ImageButton(Group parent) {
        super(parent);
        addListener(new ClickListener(GLFW.GLFW_MOUSE_BUTTON_1, ClickType.PRESS) {
            @Override
            protected void onPress(float x, float y) {
                if (clickAction != null) {
                    clickAction.run();
                }
            }
        });
    }

    public ImageButton setImage(Drawable image) {
        this.image = image;
        return setSize(image);
    }

    public ImageButton onClick(@Nullable Runnable clickAction) {
        this.clickAction = clickAction;
        return this;
    }

    @Override
    public void draw() {
        if (image != null) Global.batch.draw(image, x, y, width, height);

        // Fill.rectangleBorder(x, y, width, height, red);
    }
}
