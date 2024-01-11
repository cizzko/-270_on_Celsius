package core.Utils;

public interface Disposable extends AutoCloseable {

    boolean isDisposed();

    @Override
    void close();
}
