package core.Utils;

public interface Disposable extends AutoCloseable {

    default boolean isDisposed() { return false; }

    @Override
    void close();
}
