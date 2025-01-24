package core.input;

public interface InputListener {
    default void onResize(int width, int height) {}
}
