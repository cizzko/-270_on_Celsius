package core.input;

public interface InputListener {
    default void onFramebufferResize(int width, int height) {}

    default boolean onTouchDown(float x, float y, int button) { return false; }

    default void onTouchUp(float x, float y, int button) {}

    default void onScroll(float xOffset, float yOffset) {}

    default void onMouseMove(float x, float y) {}

    default void onMouseDragged(float x, float y) {}

    default void onKeyUp(int key, int scancode) {}

    default void onKeyDown(int key, int scancode) {}

    default void onKeyRepeat(int key, int scancode) {}

    default void onCodepoint(int codepoint) {}

    default void onMouseEnter(float x, float y) {}

    default void onMouseExit(float x, float y) {}
}
