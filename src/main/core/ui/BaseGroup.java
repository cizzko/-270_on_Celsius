package core.ui;

import core.UiScene;
import core.util.SnapshotArrayList;

import java.util.List;
import java.util.StringJoiner;

public abstract class BaseGroup<G extends BaseElement<G> & Group> extends BaseElement<G> implements Group {
    protected final SnapshotArrayList<Element> children = new SnapshotArrayList<>(new Element[4], true);

    protected BaseGroup(Group parent) {
        super(parent);
    }

    @Override
    public List<Element> children() {
        return children;
    }

    @Override
    public <E extends Element> E add(E element) {
        children.add(element);
        return element;
    }

    @Override
    public void remove(Element element) {
        children.remove(element);
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
                child.draw();
            }
        }
    }

    @Override
    public void update() {
        if (!visible()) {
            return;
        }
        super.update();
        var elem = children.begin();
        for (int i = 0, n = children.size(); i < n; i++) {
            Element child = elem[i];
            if (child.visible()) {
                try {
                    child.update();
                } catch (Exception e) {
                    UiScene.log.error("Exception while updating element {} in {}", child, this, e);
                }
            }
        }
        children.end();
    }

    @Override
    public Element hit(float hx, float hy) {
        for (Element child : children) {
            var hit = child.hit(hx, hy);
            if (hit != null) {
                return hit;
            }
        }
        return super.hit(hx, hy);
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
