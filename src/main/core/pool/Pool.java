package core.pool;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Objects;
import java.util.function.Supplier;

public final class Pool<T> {
    private final Supplier<? extends T> supplier;
    private final ObjectArrayList<T> freeObjects;
    private final int maxSize;

    public Pool(Supplier<? extends T> supplier, int maxSize) {
        this.supplier = Objects.requireNonNull(supplier);
        this.freeObjects = new ObjectArrayList<>();
        this.maxSize = maxSize;
    }

    public T obtain() {
        if (freeObjects.isEmpty()) {
            return create();
        }
        return freeObjects.pop();
    }

    public T create() {
        return supplier.get();
    }

    public void freeAll(ObjectArrayList<T> items) {
        freeObjects.addAll(items);
    }

    public void freeAndReset(T object) {
        free(object);
        if (object instanceof Poolable p) {
            p.reset();
        }
    }

    public void free(T object) {
        if (freeObjects.size() < maxSize) {
            freeObjects.add(object);
        }
    }

    public void clear() {
        freeObjects.clear();
    }
}
