package core.assets;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface EventLoopExecutor extends Executor {

    boolean isExecutorThread();

    CompletableFuture<Void> submit(Runnable action);

    <T> CompletableFuture<T> submit(Callable<? extends T> action);
}
