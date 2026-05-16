package core.UI;

import static core.Global.uiScene;

// Никакой это не диалог. Просто базовая реализация BaseGroup.
// Просто список для дочерних элементов без какой-то особой отрисовки по умолчанию
public class Dialog extends BaseGroup<Dialog> {
    protected static final int FLAG_MAXIMIZED = GROUP_LAST_FLAG << 2;

    public Dialog() {
        this(null);
    }

    public Dialog setMaximized(boolean state) {
        setFlag(FLAG_MAXIMIZED, state);
        return this;
    }

    protected Dialog(Group parent) {
        super(parent);
    }

    @Override
    public void onResize(int width, int height) {
        if ((flags & FLAG_MAXIMIZED) != 0) {
            set(0, 0, width, height);
        }

        super.onResize(width, height);
    }

    @Override
    protected void resize() {
        setSize(uiScene.root().width(), uiScene.root().height());
    }

    public void show() {
        uiScene.add(this);
    }

    public void hide() {
        uiScene.remove(this);
    }

    public void toggle() {
        uiScene.toggle(this);
    }

    // Если элемент показан это ещё не значит, что он отрисуется или будет взаимодействовать
    public boolean isShown() {
        return uiScene.contains(this);
    }
}
