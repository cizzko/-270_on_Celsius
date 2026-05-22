package core.UI;

import static core.Global.uiScene;

// Никакой это не диалог. Просто базовая реализация BaseGroup.
// Просто список для дочерних элементов без какой-то особой отрисовки по умолчанию
public class Dialog extends BaseGroup<Dialog> {
    protected static final int FLAG_MAXIMIZED = GROUP_LAST_FLAG << 2;

    protected Dialog(Group parent) {
        super(parent);
    }

    public Dialog() {
        super(null);
    }

    public Dialog setMaximized(boolean state) {
        setFlag(FLAG_MAXIMIZED, state);
        return this;
    }

    @Override
    protected void resize() {
        if ((flags & FLAG_MAXIMIZED) != 0) {
            setSize(uiScene.root().width(), uiScene.root().height());
        }
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
