package core;

import core.EventHandling.Logging.Logger;
import core.g2d.Camera2;
import core.graphic.Layer;
import core.input.InputListener;
import core.ui.Element;

import java.util.ArrayList;

import static core.Global.*;

public final class UiScene implements InputListener {
    private final Camera2 view = new Camera2();
    private final ArrayList<Element> elements = new ArrayList<>();

    public UiScene(int width, int height) {
        view.setToOrthographic(width, height);
    }

    public void add(Element element) {
        if (contains(element)) {
            return;
        }
        elements.add(element);
    }

    public void remove(Element element) {
        elements.remove(element);
    }

    public void clear() {
        elements.clear();
    }

    // Не вызывать ниоткуда!
    public void update() {
        // TODO что-то придумать с ConcurrentModificationException
        for (Element element : new ArrayList<>(elements)) {
            try {
                element.update();
            } catch (Exception e) {
                Logger.printException("Failed to update '" + element + "'", e);
            }
        }
    }

    public void draw() {
        batch.z(Layer.GUI);
        batch.matrix(view.projection);

        for (Element element : elements) {
            try {
                element.draw();
            } catch (Exception e) {
                Logger.printException("Failed to draw '" + element + "'", e);
            }
        }
    }

    public boolean contains(Element element) {
        return elements.contains(element);
    }

    public void debug() {
        for (Element element : elements) {
            System.out.println(element);
        }
    }

    @Override
    public void onResize(int width, int height) {
        view.setToOrthographic(width, height);
    }
}
