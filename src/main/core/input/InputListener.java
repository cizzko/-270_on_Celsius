package core.input;

public interface InputListener {
    default void onViewport(int x, int y, int w, int h) {}

    default void onTouchDown(float x, float y, int button) {}

    default void onTouchUp(float x, float y, int button) {}

    default void onScroll(float xOffset, float yOffset) {}

    default void onMouseMove(float x, float y) {}

    default void onMouseDragged(float x, float y) {}

    default void onKeyUp(int key, int scancode, int mods) {}

    default void onKeyDown(int key, int scancode, int mods) {}

    default void onKeyRepeat(int key, int scancode, int mods) {}

    default void onCodepoint(int codepoint, int mods) {}

    default void onMouseEnter(float x, float y) {}

    default void onMouseExit(float x, float y) {}
}
