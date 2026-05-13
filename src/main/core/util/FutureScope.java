package core.util;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FutureScope implements AutoCloseable {
    private final ArrayList<Throwable> excs = new ArrayList<>();

    public <T> T join(Future<? extends T> fut) {
        try {
            return fut.get();
        } catch (ExecutionException e) {
            excs.add(e.getCause());
        } catch (Exception e) {
            excs.add(e);
        }
        return null;
    }

    public void checkIfFailed() {
        switch (excs.size()) {
            case 0 -> {}
            case 1 ->  {
                var t = excs.getFirst();
                excs.clear();
                FutureUtil.sneakyThrow(t);
            }
            default -> {
                var t = excs.getFirst();
                for (int i = 1; i < excs.size(); ++i) {
                    t.addSuppressed(excs.get(i));
                }

                excs.clear();
                FutureUtil.sneakyThrow(t);
            }
        }
    }

    @Override
    public void close() {
        checkIfFailed();
    }
}
