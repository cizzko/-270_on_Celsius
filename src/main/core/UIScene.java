package core;

import core.ui.Element;
import core.g2d.Render;
import core.g2d.StackfulRender;
import core.graphic.Camera2;
import core.input.InputListener;
import core.ui.LayoutElement;
import core.ui.LayoutGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import static core.Global.input;

public final class UIScene implements InputListener {
    public static final Logger log = LogManager.getLogger();

    private final Camera2 view = new Camera2(1);

    private LayoutElement<?> mouseOverElement, keyboardFocus, scrollFocus, touchFocus;

    public static boolean debugBorders = false;

    public void toggleDebug() {
        debugBorders = !debugBorders;
    }

    public static final class RootElement extends LayoutGroup<RootElement> {
        RootElement() {
            super("root");
            setTouchable(false);
        }

        @Override
        protected void updateLayout() {
            for (var child : children) {
                if (child.fillParent()) {
                    child.set(0, 0, prefWidth, prefHeight);
                }
                child.layout();
            }
            layout();
        }

        @Override
        public void onFramebufferResize(int width, int height) {
            super.onFramebufferResize(width, height);
            prefSize(width, height);
        }

        @Override
        public void layout() {
            set(x, y, prefWidth, prefHeight);
        }
    }

    private final RootElement rootElement = new RootElement();

    public UIScene(int width, int height) {
        view.setToOrthographic(width, height);

        rootElement.prefSize(width, height);
    }

    public Camera2 view() {
        return view;
    }

    public RootElement root() { return rootElement; }

    public void add(LayoutElement<?> element) { rootElement.add(element);  }

    public boolean remove(LayoutElement<?> element) { return rootElement.remove(element); }

    public void toggle(LayoutElement<?> element) {
        if (contains(element)) {
            remove(element);
        } else {
            add(element);
        }
    }

    public void update(float dt) {
        updateMouseOver2();

        rootElement.update(dt);
    }

    public void draw() {
        StackfulRender.z(Render.LAYER_GUI);
        StackfulRender.camera(view);

        rootElement.draw();
    }

    public boolean contains(LayoutElement<?> element) {
        return rootElement.contains(element);
    }

    public void debug() {
        log.debug("");
        print(rootElement, 0);
        log.debug("");
    }

    static void print(LayoutElement<?> element, int nesting) {
        String indent = " ".repeat(nesting) + "|";
        log.info("{} {}", indent, element);
        log.info("{} (x={}, y={}, w={}, h={})", indent,
                element.x, element.y, element.width, element.height);
        log.info("{} pref=(w={}, h={})", indent,
                element.prefWidth(), element.prefHeight());
        log.info("{} min=(w={}, h={})", indent,
                element.minWidth(), element.minHeight());
        log.info("{} color: {}", indent, element.color);
        if (element.children().isEmpty()) {
            return;
        }
        log.info("{} children[{}]", indent, element.children().size());
        for (var child : element.children()) {
            print(child, nesting + 1);
        }
    }

    public @Nullable Element hit(float x, float y) {
        return null;
    }

    private void updateMouseOver2() {
        var mouse = input.mousePos();
        var old = mouseOverElement;
        var over = rootElement.hit(mouse.x, mouse.y);
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

    public void setFocus(LayoutElement<?> element) {
        this.touchFocus = element;
        this.scrollFocus = element;
        this.keyboardFocus = element;
    }

    public LayoutElement<?> keyboardFocus() {
        return keyboardFocus;
    }

    public void setKeyboardFocus(LayoutElement<?> keyboardFocus) {
        this.keyboardFocus = keyboardFocus;
    }

    public void setScrollFocus(LayoutElement<?> scrollFocus) {
        this.scrollFocus = scrollFocus;
    }

    public void setTouchFocus(LayoutElement<?> element) {
        this.touchFocus = element;
    }
// region InputListener


    @Override
    public void onViewport(int vpX, int vpY, int vpW, int vpH) {
        view.setToOrthographic(vpW, vpH);

        rootElement.onFramebufferResize(vpW, vpH);
    }

    @Override
    public void onTouchDown(float x, float y, int button) {
        var hit = rootElement.hit(x, y);
        if (hit != null) {
            log.trace("onTouchDown({})", hit);
            hit.onTouchDown(x, y, button);
            setFocus(hit);
        }
    }

    @Override
    public void onTouchUp(float x, float y, int button) {
        var focus = touchFocus;
        if (focus != null) {
            log.trace("onTouchUp({})", focus);
            focus.onTouchUp(x, y, button);

            if (focus == touchFocus) touchFocus = null;
        }
    }

    @Override
    public void onScroll(float xOffset, float yOffset) {
        var focus = scrollFocus;
        if (focus != null) {
            log.trace("onScroll({})", focus);
            focus.onScroll(xOffset, yOffset);
        }
    }

    @Override
    public void onMouseMove(float x, float y) {
        var focus = rootElement.hit(x, y);
        if (focus != null) {
            log.trace("onMouseMove({})", focus);
            focus.onMouseMove(x, y);
        }
    }

    @Override
    public void onMouseDragged(float x, float y) {
        var focus = touchFocus;
        if (focus != null) {
            log.trace("onMouseDragged({})", focus);
            focus.onMouseDragged(x, y);
        }
    }

    @Override
    public void onKeyUp(int key, int scancode) {
        var focus = keyboardFocus;
        if (focus != null) {
            log.trace("onKeyUp({})", focus);
            focus.onKeyUp(key, scancode);
        }
    }

    @Override
    public void onKeyDown(int key, int scancode) {
        var focus = keyboardFocus;
        if (focus != null) {
            log.trace("onKeyDown({})", focus);
            focus.onKeyDown(key, scancode);
        }
    }

    @Override
    public void onKeyRepeat(int key, int scancode) {
        var focus = keyboardFocus;
        if (focus != null) {
            log.trace("onKeyRepeat({})", focus);
            focus.onKeyRepeat(key, scancode);
        }
    }

    @Override
    public void onCodepoint(int codepoint) {
        var focus = keyboardFocus;
        if (focus != null) {
            log.trace("onCodepoint({})", focus);
            focus.onCodepoint(codepoint);
        }
    }
// endregion
}
