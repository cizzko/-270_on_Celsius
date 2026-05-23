package core;

import core.UI.BaseGroup;
import core.UI.Dialog;
import core.UI.Element;
import core.UI.Group;
import core.graphic.GuiDrawing;
import core.g2d.Fill;
import core.g2d.Render;
import core.g2d.StackfulRender;
import core.graphic.Camera2;
import core.input.InputListener;
import core.math.Vector2f;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static core.graphic.Color.*;

public final class UIScene implements InputListener {
    public static final Logger log = LogManager.getLogger();

    private final Camera2 view = new Camera2(1);
    private final Vector2f mouse = new Vector2f();

    private Element mouseOverElement;
    private Element keyboardFocus, scrollFocus, touchFocus;

    public static boolean debugBorders = false;

    public void toggleDebug() {
        debugBorders = !debugBorders;
    }

    static final class RootElement extends BaseGroup<RootElement> {
        RootElement() {
            super(null);
            setTouchable(false);
        }
    }

    private final RootElement rootElement =  new RootElement();

    public UIScene(int width, int height) {
        view.setToOrthographic(width, height);
        rootElement.setSize(width, height);
    }

    public Camera2 view() {
        return view;
    }

    public Group root() { return rootElement; }

    public <E extends Element> E add(E element) {
        return rootElement.add(element);
    }

    public boolean remove(Element element) {
        return rootElement.remove(element);
    }

    public void toggle(Element element) {
        if (contains(element)) {
            remove(element);
        } else {
            add(element);
        }
    }

    public void update(float dt) {
        updateMouseOver();
        // if (scrollFocus != null && (!scrollFocus.visible() || !contains(scrollFocus))) scrollFocus = null;
        // if (keyboardFocus != null && (!keyboardFocus.visible() || !contains(keyboardFocus))) keyboardFocus = null;
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

        rootElement.update(dt);
    }

    private static String toShortIdentifier(Element element) {
        String base = element.getClass().getSimpleName();
        String str = element.id();
        if (str == null) {
            return base;
        }
        return base + "<" + str + ">";
    }

    public void draw() {
        StackfulRender.z(Render.LAYER_GUI);
        StackfulRender.matrix(view.projection);

        rootElement.draw();

        if (debugBorders) {
            StackfulRender.pushState(() -> {
                StackfulRender.z(Render.LAYER_DEBUG);
                GuiDrawing.drawText(mouse.x, mouse.y - 32, "Pos: " + mouse);
                debugBorders();
            });
        }
    }

    private void debugBorders() {
        var lookingAt = mouseOverElement;
        var touchAt = touchFocus;
        if (touchAt != null) {
            String shortIdentifier = toShortIdentifier(touchAt);
            var size = GuiDrawing.calculateTextSize(shortIdentifier);
            Fill.rectangleBorder(touchAt.x(), touchAt.y(), touchAt.width(), touchAt.height(), red);

            float tx = touchAt.x() + touchAt.width()  - size.x;
            float ty = touchAt.y() + touchAt.height() - size.y;

            drawDebugText(tx, ty, shortIdentifier);
        }

        if (lookingAt != null && lookingAt != touchAt) {
            Fill.rectangleBorder(
                    lookingAt.x(), lookingAt.y(),
                    lookingAt.width(), lookingAt.height(), green);
            String shortIdentifier = toShortIdentifier(lookingAt);
            drawDebugText(lookingAt.x(), lookingAt.y(), shortIdentifier);
        }
    }

    private static void drawDebugText(float x, float y, String text) {
        StackfulRender.pushState(() -> {
            StackfulRender.z(Render.LAYER_DEBUG);
            GuiDrawing.drawText(x, y, text, white);
        });
    }

    public boolean contains(Element element) {
        return rootElement.contains(element);
    }

    public void debug() {
        log.debug("");
        log.debug("mouseOverElement: {}", mouseOverElement);
        log.debug("keyboardFocus: {}", keyboardFocus);
        log.debug("scrollFocus: {}", scrollFocus);
        log.debug("touchFocus: {}", touchFocus);
        for (String line : rootElement.toString().split("\n")) {
            log.debug(line);
        }
        log.debug("");
    }

    public @Nullable Element hit(float x, float y) {
        return rootElement.hit(x, y);
    }

    private void updateMouseOver() {
        Element old = mouseOverElement;
        Element over = hit(mouse.x, mouse.y);
        if (over == old) {
            return;
        }

        //log.debug("Mouse moved from {} to {}", old, over);
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
        this.touchFocus = element;
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
        if (scrollFocus != null && scrollFocus.isDescendantOf(Predicate.isEqual(element))) {
            setScrollFocus(null);
        }
        if (keyboardFocus != null && keyboardFocus.isDescendantOf(Predicate.isEqual(element))) {
            setKeyboardFocus(null);
        }
    }

// region InputListener

    @Override
    public void onResize(int width, int height) {
        // if (Global.postEffect.use) {
        //     Global.postEffect.resize(width, height);
        // }
        view.setToOrthographic(width, height);
        rootElement.onResize(width, height);
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
        if (focus != null) {
            log.debug("onKeyUp({})", focus);
            focus.onKeyUp(key, scancode);
        }
    }

    @Override
    public void onKeyDown(int key, int scancode) {
        Element focus = keyboardFocus;
        if (focus != null) {
            log.debug("onKeyDown({})", focus);
            focus.onKeyDown(key, scancode);
        }
    }

    @Override
    public void onKeyRepeat(int key, int scancode) {
        Element focus = keyboardFocus;
        if (focus != null) {
            log.debug("onKeyRepeat({})", focus);
            focus.onKeyRepeat(key, scancode);
        }
    }

    @Override
    public void onCodepoint(int codepoint) {
        Element focus = keyboardFocus;
        if (focus != null) {
            log.debug("onCodepoint({})", focus);
            focus.onCodepoint(codepoint);
        }
    }
// endregion
}
