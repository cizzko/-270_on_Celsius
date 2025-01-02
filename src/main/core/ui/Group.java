package core.ui;

import core.g2d.Drawable;

import java.util.List;

public interface Group extends Element {
    List<Element> children();

    <E extends Element> E add(E element);

    void remove(Element element);

    // region ковариантное переопределение
    @Override
    Group set(int x, int y, int width, int height);

    @Override
    Group setSize(int size);

    @Override
    Group setSize(int width, int height);

    @Override
    Group setX(int x);

    @Override
    Group setY(int y);

    @Override
    Group setPosition(int x, int y);

    @Override
    Group setVisible(boolean state);

    // endregion
    // region Дополнительные методы
    default Panel addPanel(int x, int y, int width, int height) {
        return add(new Panel(this))
                .set(x, y, width, height);
    }

    default Panel addPanel() {
        return add(new Panel(this));
    }

    default Button addButton(Runnable onClick) {
        return add(new Button(this))
                .onClick(onClick);
    }

    default ToggleButton addToggleButton(Runnable onClick) {
        return add(new ToggleButton(this))
                .onClick(onClick);
    }

    default ImageButton addImageButton(Runnable onClick) {
        return add(new ImageButton(this))
                .onClick(onClick);
    }

    default Slider addSlider(int max, Slider.MoveListener onMove) {
        return add(new Slider(this))
                .setMax(max)
                .onMove(onMove);
    }

    default ImageElement addImage() {
        return add(new ImageElement(this));
    }

    default ImageElement addImage(int x, int y, Drawable path) {
        return addImage()
                .setPosition(x, y)
                .setImage(path);
    }
    // endregion
}
