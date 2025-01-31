package core.ui;

import static core.Global.*;

// Никакой это не диалог. Просто базовая реализация BaseGroup.
// Просто список для дочерних элементов без какой-то особой отрисовки по умолчанию
public class Dialog extends BaseGroup<Dialog> {
    public Dialog() {
        this(null);
    }

    protected Dialog(Group parent) {
        super(parent);
    }

    public void show() {
        uiScene.add(this);
    }

    public void hide() {
        uiScene.remove(this);
    }

    public void toggle() {
        if (uiScene.contains(this)) {
            hide();
        } else {
            show();
        }
    }

    public boolean isShown() { // Если элемент показан это ещё не значит, что он отрисуется или будет взаимодействовать
        return uiScene.contains(this);
    }
}
