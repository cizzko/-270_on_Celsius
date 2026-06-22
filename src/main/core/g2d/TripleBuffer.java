package core.g2d;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

public final class TripleBuffer {
    final RenderList[] storage;

    private int write_idx  = 0;
    private int read_idx   = 1;
    private int shared_idx = 2;
    private boolean has_new_data = false;

    private static final VarHandle SHARED_IDX;
    private static final VarHandle HAS_NEW_DATA;
    private static final VarHandle STORAGE_ARRAY;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            SHARED_IDX = lookup.findVarHandle(TripleBuffer.class, "shared_idx", int.class)
                    .withInvokeExactBehavior();
            HAS_NEW_DATA = lookup.findVarHandle(TripleBuffer.class, "has_new_data", boolean.class)
                    .withInvokeExactBehavior();
            STORAGE_ARRAY = MethodHandles.arrayElementVarHandle(RenderList[].class)
                    .withInvokeExactBehavior();
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public TripleBuffer(RenderList[] storage) {
        // assert storage.length == 3;
        this.storage = storage;
    }

    public RenderList produce(RenderList value) {
        Objects.requireNonNull(value);
        STORAGE_ARRAY.setRelease(storage, write_idx, value);

        write_idx = (int) SHARED_IDX.getAndSetRelease(this, write_idx);

        HAS_NEW_DATA.setRelease(this, true);
        return (RenderList) STORAGE_ARRAY.getAcquire(storage, write_idx);
    }

    public boolean tryConsume() {
        boolean hasNewData = (boolean) HAS_NEW_DATA.getAcquire(this);
        if (!hasNewData) {
            return false;
        }

        HAS_NEW_DATA.setOpaque(this, false);

        read_idx = (int) SHARED_IDX.getAndSetAcquire(this, read_idx);
        return true;
    }

    public RenderList peek() {
        var acquire = (RenderList) STORAGE_ARRAY.getAcquire(storage, read_idx);
        Objects.requireNonNull(acquire);
        return acquire;
    }
}
