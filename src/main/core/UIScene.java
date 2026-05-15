package core;

import core.UI.Dialog;
import core.UI.Element;
import core.g2d.Camera2;
import core.graphic.Layer;
import core.input.InputListener;
import core.math.Vector2f;
import core.util.SnapshotArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static core.Global.batch;

public final class UIScene implements InputListener {
    public static final Logger log = LogManager.getLogger();

    private final Camera2 view = new Camera2();
    private final Vector2f mouse = new Vector2f();
    private final SnapshotArrayList<Element> elements = new SnapshotArrayList<>(new Element[16], true);

    private Element mouseOverElement;
    private Element keyboardFocus, scrollFocus, touchFocus;

    public UIScene(int width, int height) {
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
    public void update(float dt) {
        updateMouseOver();
        if (scrollFocus != null && (!scrollFocus.visible() || !contains(scrollFocus))) scrollFocus = null;
        if (keyboardFocus != null && (!keyboardFocus.visible() || !contains(keyboardFocus))) keyboardFocus = null;
        Element curr = scrollFocus;
        if (curr != null) {
            while (curr != null && curr.parent() != null) {
                if (!curr.visible()) {
                    scrollFocus = null;
                    break;
                }
                curr = curr.parent();
            }
        }
        var elem = elements.begin();
        for (int i = 0, n = elements.size(); i < n; i++) {
            Element element = elem[i];
            try {
                element.update(dt);
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
            if (element.visible()) {
                try {
                    element.draw();
                } catch (Exception e) {
                    log.error("Failed to draw '{}'", element, e);
                }
            }
        }
    }

    public boolean contains(Element element) {
        return elements.contains(element);
    }

    public void debug() {
        log.debug("");
        log.debug("mouseOverElement: {}", mouseOverElement);
        log.debug("keyboardFocus: {}", keyboardFocus);
        log.debug("scrollFocus: {}", scrollFocus);
        log.debug("touchFocus: {}", touchFocus);
        for (Element element : elements) {
            for (String s : element.toString().split("\n")) {
                log.debug(s);
            }
        }
        log.debug("");
    }

    public @Nullable Element hit(float x, float y) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            var element = elements.get(i);
            if (!element.visible()) {
                continue;
            }
            var hit = element.hit(x, y);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    private void updateMouseOver() {
        Element old = mouseOverElement;
        Element over = hit(mouse.x, mouse.y);
        if (over == old) {
            return;
        }

        log.debug("Mouse moved from {} to {}", old, over);
        if (old != null) {
            old.onMouseExit(mouse.x, mouse.y);
        }
        if (over != null) {
            over.onMouseEnter(mouse.x, mouse.y);
        }
        this.mouseOverElement = over;
    }

    public boolean hasDialog() {
        return scrollFocus instanceof Dialog || (keyboardFocus != null && keyboardFocus.isDescendantOf(e -> e instanceof Dialog));
    }

    public void setFocus(Element element) {
        this.scrollFocus = element;
        this.keyboardFocus = element;
    }

    public Element getScrollFocus() {
        return scrollFocus;
    }

    public Element getKeyboardFocus() {
        return keyboardFocus;
    }

    public void setKeyboardFocus(Element keyboardFocus) {
        this.keyboardFocus = keyboardFocus;
    }

    public void setScrollFocus(Element scrollFocus) {
        this.scrollFocus = scrollFocus;
    }

    public void setTouchFocus(Element element) {
        this.touchFocus = element;
    }

    public void unfocus(Element element) {
        if (touchFocus == element) {
            element.onTouchUp(Float.MIN_VALUE, Float.MIN_VALUE, -1);
            touchFocus = null;
        }
        if (scrollFocus != null && scrollFocus.isDescendantOf(Predicate.isEqual(element))) setScrollFocus(null);
        if (keyboardFocus != null && keyboardFocus.isDescendantOf(Predicate.isEqual(element))) setKeyboardFocus(null);
    }

    // region InputListener

    @Override
    public void onResize(int width, int height) {
        view.setToOrthographic(width, height);
    }

    @Override
    public boolean onTouchDown(float x, float y, int button) {
        view.unproject(mouse.set(x, y));
        var hit = hit(mouse.x, mouse.y);
        if (hit != null) {
            log.debug("onTouchDown({})", hit);
            hit.onTouchDown(mouse.x, mouse.y, button);
        }
        return true;
    }

    @Override
    public void onTouchUp(float x, float y, int button) {
        view.unproject(mouse.set(x, y));
        Element focus = touchFocus;
        if (focus != null) {
            log.debug("onTouchUp({})", focus);
            focus.onTouchUp(mouse.x, mouse.y, button);

            if (focus == touchFocus) {
                setTouchFocus(null);
            }
        }
    }

    @Override
    public void onScroll(float xOffset, float yOffset) {
        Element focus = scrollFocus;
        if (focus == null && !elements.isEmpty()) {
            focus = elements.getLast();
        }
        if (focus != null) {
            log.debug("onScroll({})", focus);
            focus.onScroll(xOffset, yOffset);
        }
    }

    @Override
    public void onMouseMove(float x, float y) {
        view.unproject(mouse.set(x, y));
        Element focus = hit(mouse.x, mouse.y);
        if (focus != null) {
            // log.trace("onMouseMove({})", focus);
            focus.onMouseMove(mouse.x, mouse.y);
        }
    }

    @Override
    public void onMouseDragged(float x, float y) {
        view.unproject(mouse.set(x, y));
        Element focus = touchFocus;
        if (focus != null) {
            log.debug("onMouseDragged({})", focus);
            focus.onMouseDragged(mouse.x, mouse.y);
        }
    }

    @Override
    public void onKeyUp(int key, int scancode) {
        Element focus = keyboardFocus;
        if (focus == null && !elements.isEmpty()) {
            focus = elements.getLast();
        }
        if (focus != null) {
            log.debug("onKeyUp({})", focus);
            focus.onKeyUp(key, scancode);
        }
    }

    @Override
    public void onKeyDown(int key, int scancode) {
        Element focus = keyboardFocus;
        if (focus == null && !elements.isEmpty()) {
            focus = elements.getLast();
        }
        if (focus != null) {
            log.debug("onKeyDown({})", focus);
            focus.onKeyDown(key, scancode);
        }
    }

    @Override
    public void onKeyRepeat(int key, int scancode) {
        Element focus = keyboardFocus;
        if (focus == null && !elements.isEmpty()) {
            focus = elements.getLast();
        }
        if (focus != null) {
            log.debug("onKeyRepeat({})", focus);
            focus.onKeyRepeat(key, scancode);
        }
    }

    @Override
    public void onCodepoint(int codepoint) {
        Element focus = keyboardFocus;
        if (focus == null && !elements.isEmpty()) {
            focus = elements.getLast();
        }
        if (focus != null) {
            log.debug("onCodepoint({})", focus);
            focus.onCodepoint(codepoint);
        }
    }
// endregion
}
