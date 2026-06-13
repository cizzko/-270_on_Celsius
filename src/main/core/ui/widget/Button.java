package core.ui.widget;

import core.lang.LangTranslation;
import core.ui.ClickListener;
import core.ui.Style;
import core.g2d.Fill;
import core.graphic.Color;
import core.ui.Align;
import core.ui.Table;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public final class Button extends Table {
    public final Color disabledColor = Color.CLEAR.copy();
    public float borderWidth;

    public boolean isClickable = true, isUnderMouse, isClicked, oneShot;
    public final Label label;
    public Consumer<? super Button> action;

    public Button(@Nullable String id, Style.TextButton style) {
        super(id);
        this.borderWidth = style.borderWidth;
        this.color.set(style.backgroundColor);
        this.disabledColor.set(style.disabledColor);
        minWidth(style.minWidth);
        prefHeight(style.prefHeight);

        cell(this.label = new Label(id, style.text), cell -> {
            cell.widget.setTouchable(false);
            cell.grow();
            cell.pad(16);
            cell.align(Align.LEFT);
        });
    }

    public Button label(String text) {
        this.label.text(text);
        return this;
    }

    public Button action(Consumer<? super Button> action) {
        addListener(new ClickListener(GLFW.GLFW_MOUSE_BUTTON_1, null) {
            @Override
            protected void onPress(float x, float y) {
                if (!isClickable) {
                    return;
                }

                isClicked = true;
                action.accept(Button.this);
                if (oneShot) {
                    isClickable = false;
                }
            }

            @Override
            public void onMouseEnter(float x, float y) {
                isUnderMouse = true;
            }

            @Override
            public void onMouseExit(float x, float y) {
                isUnderMouse = false;
            }

            @Override
            protected void onRelease(float x, float y) {
                if (!isClickable) {
                    return;
                }
                isClicked = false;
            }
        });
        return this;
    }

    public Button action(Runnable action) {
        return action(b -> action.run());
    }

    @Override
    public void drawThis() {

        float bw = borderWidth;
        if (bw > 0) {
            Fill.rectangleBorder(x, y, width, height, borderWidth, color.rgba8888());
        } else {
            Fill.rect(x, y, width, height, color);
        }

        // TODO хардкорд баттона
        if (isUnderMouse) {
            Fill.rect(
                    x + borderWidth, y + borderWidth,
                    width - 2*borderWidth, height - 2*borderWidth, Color.rgba8888(34,34, 34, 150));
        }

        if (isClicked)
            Fill.rect(x, y, width, height, disabledColor);
    }

    public Button labelTranslation(@LangTranslation.Translation String labelTranslation) {
        this.label.translation(labelTranslation);
        return this;
    }

    public void reset() {
        isClicked = false;
        isClickable = true;
    }

    public void clicked(boolean state) {
        isClicked = state;
        isClickable = !state;
    }
}
