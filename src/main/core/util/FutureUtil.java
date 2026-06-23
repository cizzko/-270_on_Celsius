package core.util;

import java.util.concurrent.*;

public class FutureUtil {
    public static <T> T join(Future<? extends T> future) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (ExecutionException | CancellationException e) {
                    uncheckedThrow(e.getCause());
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static <E extends Throwable> void uncheckedThrow(Throwable e) throws E {
        throw (E) e;
    }
}
