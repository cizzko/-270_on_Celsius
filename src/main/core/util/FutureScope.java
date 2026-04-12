package core.util;

import java.util.ArrayList;
import java.util.concurrent.Future;

public class FutureScope implements AutoCloseable {
    private final ArrayList<Throwable> excs = new ArrayList<>();

    public <T> T join(Future<? extends T> fut) {
        try {
            return fut.get();
        } catch (Throwable t) {
            excs.add(t); return null;
        }
    }

    public void close() {
        checkIfFailed();
    }

    public void checkIfFailed() {


        switch (excs.size()) {
            case 0 -> {}
            case 1 ->  {
                var t = excs.getFirst();
                excs.clear();
                FutureExc.sneakyThrow(t);
            }
            default -> {
                var t = excs.getFirst();
                for (int i = 1; i < excs.size(); ++i) {
                    t.addSuppressed(excs.get(i));
                }

                excs.clear();
                FutureExc.sneakyThrow(t);
            }
        }
    }
}