package core.ui;

import core.input.InputListener;
import core.math.Point2i;
import core.math.Vector2f;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Element extends InputListener {
    @Nullable String id();

    /// @return `null` если элемент не добавлен в сцену. У всех активных элементов сцены есть родитель
    @Nullable Group parent();

    // region Итоговые размеры
    float x();
    float y();
    float width();
    float height();
    // endregion

    // region Размеры для разметки
    // min <= pref <= max
    float minWidth();
    float minHeight();
    float prefWidth();
    float prefHeight();
    float maxWidth();
    float maxHeight();
    // endregion

    /// Определяет будет ли элемент в сцене
    boolean visible();
    boolean fillParent();

    void draw();

    default void layout() {}

    void update(float dt);
    boolean remove();

    boolean horizontalClip();
    boolean verticalClip();

    Element addListener(InputListener listener);
    List<InputListener> listeners();

    Element id(@Nullable String id);

    Element setParent(@Nullable Group parent);
    Element setFillParent(boolean state);
    Element setVisible(boolean state);
    Element toggleVisibility();

    Element setTouchable(boolean state);

    Element setHotkey(int key, Runnable action);
    Element setClickKey(int key, ClickType type, Runnable action);

    @Nullable Element hit(float x, float y);
    default @Nullable Element hit(Point2i point)  { return hit(point.x, point.y); }
    default @Nullable Element hit(Vector2f point) { return hit(point.x, point.y); }

    @SuppressWarnings("unchecked")
    default <E extends Element> E as() { return (E) this; }
}
