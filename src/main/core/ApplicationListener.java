package core;

public interface ApplicationListener {
    // Выполняется каждый такт
    default void update() {}
}
