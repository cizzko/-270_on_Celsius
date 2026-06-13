package core.ui;

import core.UIScene;
import core.util.SnapshotArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class LayoutGroup<This extends LayoutGroup<This>>
        extends LayoutElement<This>
        implements Group {

    protected static final int FLAG_TOUCHABLE_CHILDREN = ELEMENT_LAST_FLAG << 1;

    protected static final int GROUP_LAST_FLAG = FLAG_TOUCHABLE_CHILDREN;

    protected final SnapshotArrayList<LayoutElement<?>> children = new SnapshotArrayList<>(4);

    protected LayoutGroup(@Nullable String id) {
        super(id);
        flags |= FLAG_TOUCHABLE_CHILDREN;
    }

    public final List<LayoutElement<?>> children() { return children; }

    public This add(LayoutElement<?> element) {
        if (contains(element)) {
            return as();
        }

        element.setParent(this);
        children.add(element);
        return as();
    }

    public boolean remove(LayoutElement<?> element) {
        boolean res = children.remove(element);
        if (res) {
            element.setParent(null);
        }
        return res;
    }

    protected void drawThis() { }

    protected void drawAfterMe() { }

    @Override
    public void draw() {
        drawThis();
        drawAfterMe();
        for (var child : children) {
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
        super.update(dt);

        updateLayout();

        Object[] elem = children.begin();
        for (int i = 0, n = children.size(); i < n; i++) {
            if (elem[i] instanceof Element child) {
                try {
                    child.update(dt);
                } catch (Exception e) {
                    UIScene.log.error("Exception while updating element: {}", child, e);
                }
            }
        }
        children.end();
    }

    protected void updateLayout() {
        if (fillParent()) {
            width  = parent.width();
            height = parent.height();
        }

        for (LayoutElement<?> child : children) {
            child.layout();
        }
        layout();
    }

    @Override
    public LayoutElement<?> hit(float hx, float hy) {
        if ((flags & FLAG_TOUCHABLE_CHILDREN) != 0) {
            for (int i = children.size() - 1; i >= 0; i--) {
                var child = children.get(i);
                var hit = child.hit(hx, hy);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return super.hit(hx, hy);
    }

    @Override
    public final This setTouchableChildren(boolean state) {
        setFlag(FLAG_TOUCHABLE_CHILDREN, state);
        return as();
    }
}
