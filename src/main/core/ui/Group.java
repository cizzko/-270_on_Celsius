package core.ui;

import core.input.InputListener;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public interface Group extends Element {
    List<LayoutElement<?>> children();

    Group add(LayoutElement<?> element);
    default <E> Group add(E element, Consumer<? super E> action) {
        action.accept(element);
        add((LayoutElement<?>) element); // Костыль
        return as();
    }

    boolean remove(LayoutElement<?> element);
    default boolean contains(LayoutElement<?> element) { return children().contains(element); }

    Group setTouchableChildren(boolean state);

    // region ковариантное переопределение

    Group addListener(InputListener listener);
    Group setParent(@Nullable Group parent);
    Group id(String id);
    Group setVisible(boolean state);
    Group toggleVisibility();
    Group setTouchable(boolean state);
    Group setHotkey(int key, Runnable action);

    // endregion
}
