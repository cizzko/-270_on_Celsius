package core.util;

public interface Disposable extends AutoCloseable {

    default boolean isDisposed() { return false; }

    @Override
    void close();
}
