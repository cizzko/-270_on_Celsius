package core;

public interface ApplicationListener {
    // Выполняется каждый такт
    default void update() {}

    default void suspend() {}

    default void resume() {}
}
