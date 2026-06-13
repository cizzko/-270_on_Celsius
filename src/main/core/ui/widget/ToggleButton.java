package core.ui.widget;

import core.lang.LangTranslation;
import core.ui.ClickType;
import core.ui.Style;
import core.g2d.Drawable;
import core.ui.Align;
import core.ui.Table;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static core.ui.widget.Widgets.*;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public final class ToggleButton extends Table {

    public boolean isClickable = true, isClicked, oneShot;
    public final Label label;
    public final Image image;

    public Consumer<? super ToggleButton> action;

    public ToggleButton(@Nullable String id, Style.ToggleButton style, boolean defaultState) {
        super(id);
        setTouchable(false);

        this.color.set(style.backgroundColor);

        Drawable checkUp = style.checkUp;
        Drawable checkDown = style.checkDown;

        this.image = image(id, defaultState ? checkUp : checkDown);
        image.fixedSize(32, 32);
        image.setClickKey(GLFW_MOUSE_BUTTON_1, ClickType.PRESS, () -> {
            if (!isClickable) {
                return;
            }
            isClicked = !isClicked;
            image.drawable(isClicked ? checkUp : checkDown);

            if (action != null) action.accept(this);

            if (isClicked && oneShot) {
                isClickable = false;
            }
        });

        pad(16);

        cell(panel(id, new Style.Panel() {
            {
                borderWidth = style.borderOffset;
                backgroundColor = style.backgroundColor;
            }
        }), cell -> {
            cell.align(Align.LEFT);
            cell.expand();
            cell.fixed(cell.widget.borderWidth*2 + image.prefWidth,
                    cell.widget.borderWidth*2 + image.prefWidth);

            cell.widget.setTouchable(false);
            cell.widget.cell(image);
        });

        cell(this.label = new Label(id, style.text), cell -> {
            cell.widget.setTouchable(false);
            cell.expand();
            cell.padLeft(16);
            cell.align(Align.LEFT);
        });
    }

    public ToggleButton action(Runnable action) {
        this.action = b -> action.run();
        return this;
    }

    public ToggleButton action(Consumer<? super ToggleButton> action) {
        this.action = action;
        return this;
    }

    public ToggleButton label(String label) {
        this.label.text(label);
        return this;
    }

    public ToggleButton labelTranslation(@LangTranslation.Translation String labelTranslation) {
        this.label.translation(labelTranslation);
        return this;
    }
}
