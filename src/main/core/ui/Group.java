package core.ui;

import core.Utils.Sized;
import core.g2d.Drawable;

import java.util.List;
import java.util.function.Consumer;

public interface Group extends Element {
    List<Element> children();

    <E extends Element> E add(E element);

    void remove(Element element);

    // region ковариантное переопределение


    @Override
    Group setId(String id);
    @Override
    Group setX(float x);
    @Override
    Group setY(float y);
    @Override
    Group setWidth(float width);
    @Override
    Group setHeight(float height);
    @Override
    Group setPosition(float x, float y);
    @Override
    Group setSize(Sized sized);
    @Override
    Group setSize(float width, float height);
    @Override
    Group set(float x, float y, float width, float height);
    @Override
    Group setVisible(boolean visible);
    @Override
    Group toggleVisibility();

    // endregion
    // region Дополнительные методы
    default Panel addPanel(Style.Panel style, float x, float y, float width, float height) {
        return add(new Panel(this, style))
                .set(x, y, width, height);
    }

    default Button addButton(Style.TextButton style, Consumer<Button> onClick) {
        return add(new Button(this, style))
                .onClick(onClick);
    }

    default Button addButton(Style.TextButton style, Runnable onClick) {
        return add(new Button(this, style))
                .onClick(onClick);
    }

    default ToggleButton addToggleButton(Style.ToggleButton style, Runnable onClick) {
        return add(new ToggleButton(this, style))
                .onClick(onClick);
    }

    default ImageButton addImageButton(Runnable onClick) {
        return add(new ImageButton(this))
                .onClick(onClick);
    }

    default Slider addSlider(Style.Slider style, int min, int max, Slider.MoveListener onMove) {
        return add(new Slider(this, style))
                .setBounds(min, max)
                .onMove(onMove);
    }

    default ImageElement addImage(float x, float y, Drawable path) {
        return add(new ImageElement(this))
                .setPosition(x, y)
                .setImage(path);
    }
    // endregion
}
