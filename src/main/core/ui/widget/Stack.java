package core.ui.widget;

import core.ui.LayoutGroup;
import org.jetbrains.annotations.Nullable;

public final class Stack extends LayoutGroup<Stack> {

    public Stack(@Nullable String id) {
        super(id);
        setTouchable(false);
    }

    @Override
    public void layout() {
        for (var child : children) {
            // if (child.fillParent())
            child.set(x, y, width, height);
        }
    }

    @Override
    public float minWidth() {
        float min = minWidth;
        for (var child : children) min = Math.max(min, child.minWidth());
        return min;
    }

    @Override
    public float minHeight() {
        float min = minWidth;
        for (var child : children) min = Math.max(min, child.minWidth());
        return min;
    }

    @Override
    public float prefWidth() {
        float min = prefWidth;
        for (var child : children) min = Math.max(min, child.prefWidth());
        return min;
    }

    @Override
    public float prefHeight() {
        float min = prefHeight;
        for (var child : children) min = Math.max(min, child.prefHeight());
        return min;
    }

    @Override
    public float maxWidth() {
        float min = maxWidth;
        for (var child : children) min = Math.max(min, child.maxWidth());
        return min;
    }

    @Override
    public float maxHeight() {
        float min = maxHeight;
        for (var child : children) min = Math.max(min, child.maxHeight());
        return min;
    }
}
