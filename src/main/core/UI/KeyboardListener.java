package core.UI;

import core.input.InputListener;

public class KeyboardListener implements InputListener {
    public int key;
    public Runnable action;

    public KeyboardListener(int key, Runnable action) {
        this.key = key;
        this.action = action;
    }

    @Override
    public void onKeyDown(int k, int scancode) {
        if (key == k) {
            action.run();
        }
    }
}
