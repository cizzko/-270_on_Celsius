package core.UI;

import core.UIScene;
import core.util.SnapshotArrayList;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public abstract class BaseGroup<G extends BaseElement<G> & Group> extends BaseElement<G> implements Group {
    protected static final int FLAG_TOUCHABLE_CHILDREN = ELEMENT_LAST_FLAG << 2;

    protected static final int GROUP_LAST_FLAG = FLAG_TOUCHABLE_CHILDREN;

    protected final SnapshotArrayList<Element> children = new SnapshotArrayList<>(4);

    protected BaseGroup(Group parent) {
        super(parent);
        flags |= FLAG_TOUCHABLE_CHILDREN;
    }

    @Override
    public final List<Element> children() {
        return children;
    }

    @Override
    public <E extends Element> E add(E element) {
        if (contains(element)) {
            return element;
        }

        element.setParent(this);
        children.add(element);
        return element;
    }

    @Override
    public boolean remove(Element element) {
        boolean res = children.remove(element);
        if (res) {
            element.setParent(null);
        }
        return res;
    }

    protected void drawThis() {}

    @Override
    public void draw() {
        if (!visible()) {
            return;
        }
        drawThis();
        for (Element child : children) {
            if (child.visible()) {
                try {
                    child.draw();
                } catch (Exception e) {
                    UIScene.log.error("Failed to draw element: {}", child, e);
                }
            }
        }
    }

    @Override
    public void update(float dt) {
        if (!visible()) {
            return;
        }
        super.update(dt);
        Object[] elem = children.begin();
        for (int i = 0, n = children.size(); i < n; i++) {
            if (elem[i] instanceof Element child) {
                if (child.visible()) {
                    try {
                        child.update(dt);
                    } catch (Exception e) {
                        UIScene.log.error("Exception while updating element: {}", child, e);
                    }
                }
            }
        }
        children.end();
    }

    @Override
    public Element hit(float hx, float hy) {
        if ((flags & FLAG_TOUCHABLE_CHILDREN) != 0) {
            for (Element child : children) {
                var hit = child.hit(hx, hy);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return super.hit(hx, hy);
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        for (Element child : children) {
            child.onResize(width, height);
        }
    }

    @Override
    public final G setTouchableChildren(boolean state) {
        setFlag(FLAG_TOUCHABLE_CHILDREN, state);
        return as();
    }

    @Override
    protected String toStringImpl(int indent) {
        StringJoiner ch = new StringJoiner("\n", "\n", "")
                .setEmptyValue("");
        var children = children();
        for (int i = 0; i < children.size(); i++) {
            Element el = children.get(i);
            String tab = " ".repeat(indent);
            String str = el instanceof BaseElement<?> d ? d.toStringImpl(indent + 1) : el.toString();
            ch.add(tab + "[" + i + "] " + str);
        }
        return super.toStringImpl(indent) + ch;
    }
}
