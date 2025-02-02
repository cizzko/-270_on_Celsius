package core;

import core.g2d.Camera2;
import core.graphic.Layer;
import core.input.InputListener;
import core.ui.Element;
import core.util.SnapshotArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

import static core.Global.*;

public final class UiScene implements InputListener {
    public static final Logger log = LogManager.getLogger();

    private final Camera2 view = new Camera2();
    private final SnapshotArrayList<Element> elements = new SnapshotArrayList<>(new Element[16], true);

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
        var elem = elements.begin();
        for (int i = 0, n = elements.size(); i < n; i++) {
            Element element = elem[i];
            try {
                element.update();
            } catch (Exception e) {
                log.error("Failed to update '{}'", element, e);
            }
        }
        elements.end();
    }

    public void draw() {
        batch.z(Layer.GUI);
        batch.matrix(view.projection);

        for (Element element : elements) {
            if (!element.visible()) {
                continue;
            }

            try {
                element.draw();
            } catch (Exception e) {
                log.error("Failed to draw '{}'", element, e);
            }
        }
    }

    public boolean contains(Element element) {
        return elements.contains(element);
    }

    public void debug() {
        for (Element element : elements) {
            log.debug(element);
        }
    }

    @Override
    public void onResize(int width, int height) {
        view.setToOrthographic(width, height);
    }
}
