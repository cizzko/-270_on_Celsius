package core.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public abstract class BaseGroup<G extends BaseElement<G> & Group> extends BaseElement<G> implements Group {
    // TODO специальный лист с защитой от ConcurrentModificationException
    protected ArrayList<Element> children;

    protected BaseGroup(Group parent) {
        super(parent);
    }

    private ArrayList<Element> initChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

    @Override
    public List<Element> children() {
        return children != null ? children : List.of();
    }

    @Override
    public <E extends Element> E add(E element) {
        initChildren().add(element);
        return element;
    }

    @Override
    public void remove(Element element) {
        if (children != null) {
            children.remove(element);
        }
    }

    @Override
    public void draw() {
        if (!visible) {
            return;
        }
        if (children != null) {
            for (Element child : children) {
                child.draw();
            }
        }
    }

    @Override
    public void update() {
        if (!visible) {
            return;
        }
        if (children != null) {
            for (Element child : new ArrayList<>(children)) {
                child.update();
            }
        }
    }

    @Override
    public Element hit(int hx, int hy) {
        if (children != null) {
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
    public String toString() {
        return printTree(1);
    }

    protected String printTree(int nesting) {
        StringJoiner ch = new StringJoiner("\n", "\n", "")
                .setEmptyValue("");
        var children = children();
        for (int i = 0; i < children.size(); i++) {
            Element el = children.get(i);
            String tab = " ".repeat(nesting);
            String str = el instanceof BaseGroup<?> d ? d.printTree(nesting + 1) : el.toString();
            ch.add(tab + i + ": " + str);
        }
        return super.toString() + ch;
    }
}
