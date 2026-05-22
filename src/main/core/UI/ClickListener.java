package core.UI;

import core.input.InputListener;

import static core.Global.input;

public class ClickListener implements InputListener {
    private int button;
    private ClickType clickType;
    private boolean down;
    protected long tapCountInterval = (long) (0.5f * 1e9f);
    protected long lastTapTime;
    protected int tapCount;

    public ClickListener(int button, ClickType clickType) {
        this.button = button;
        this.clickType = clickType;
    }

    @Override
    public boolean onTouchDown(float x, float y, int button) {
        if (button != this.button) {
            return false;
        }
        if (down) {
            return false;
        }
        if (clickType == null || clickType == ClickType.PRESS) {
            onPress(x, y);
        }
        down = true;
        return true;
    }

    @Override
    public void onMouseDragged(float x, float y) {
        if (down && !input.clicked(button)) {
            down = false;
        }
    }

    @Override
    public void onTouchUp(float x, float y, int button) {
        if (button == this.button) {
            if (clickType == null || clickType == ClickType.RELEASE) {
                long time = System.nanoTime();
                if (time - lastTapTime > tapCountInterval) {
                    tapCount = 0;
                }
                tapCount++;
                lastTapTime = time;
                onRelease(x, y);
            }
        }
        down = false;
    }

    protected void onPress(float x, float y) {
    }

    protected void onRelease(float x, float y) {
    }
}
