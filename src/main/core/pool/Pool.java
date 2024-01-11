package core.pool;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.Supplier;

public final class Pool<T> {
    private final Supplier<? extends T> supplier;
    private final ArrayDeque<T> freeObjects;
    private final int maxSize;

    public Pool(Supplier<? extends T> supplier, int maxSize) {
        this.supplier = Objects.requireNonNull(supplier);
        this.freeObjects = new ArrayDeque<>();
        this.maxSize = maxSize;
    }

    public T obtain() {
        if (freeObjects.isEmpty()) {
            return supplier.get();
        }
        return freeObjects.pollLast();
    }

    public void free(T object) {
        Objects.requireNonNull(object);
        if (freeObjects.size() < maxSize) {
            freeObjects.addLast(object);
        }
        if (object instanceof Poolable p) p.reset();
    }
}
