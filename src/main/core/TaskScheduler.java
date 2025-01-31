package core;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.*;

public class TaskScheduler {
    // TODO тут нужна MPSC очередь, желательно фиксированного размера
    private final ArrayList<TaskImpl<?>> tasks = new ArrayList<>(); // synchronized
    private volatile boolean running = true;

    public void post(Runnable task, float delay) {
        post(() -> {
            task.run();
            return null;
        }, delay);
    }

    public <T> CompletableFuture<T> post(Callable<? extends T> task) {
        return post(task, 0);
    }

    public CompletableFuture<Void> post(Runnable task) {
        return post(() -> {
            task.run();
            return null;
        }, 0);
    }

    public <T> CompletableFuture<T> post(Callable<? extends T> task, float delay) {
        Objects.requireNonNull(task);
        if (!running) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("TaskScheduler has been shutdown: " + task));
        }

        var future = new TaskImpl<T>(delay, task);
        synchronized (tasks) {
            tasks.add(future);
        }
        return future.result;
    }

    public void executeAll() {
        ArrayList<TaskImpl<?>> workQueue;
        synchronized (tasks) {
            workQueue = new ArrayList<>(tasks);
        }
        var handled = new ArrayList<TaskImpl<?>>();
        for (TaskImpl<?> task : workQueue) {
            task.delay -= Time.delta;

            if (task.delay <= 0) {
                handled.add(task);

                task.run();
            }
        }
        if (!handled.isEmpty()) {
            synchronized (tasks) {
                tasks.removeAll(handled);
            }
        }
    }

    public CompletableFuture<Void> execute(Runnable runnable) {
        if (Global.app.isMainThread()) {
            try {
                runnable.run();
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        return post(runnable);
    }

    public <T> CompletableFuture<T> execute(Callable<? extends T> callable) {
        if (Global.app.isMainThread()) {
            try {
                return CompletableFuture.completedFuture(callable.call());
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        return post(callable);
    }

    public void shutdown() {
        running = false;

        synchronized (tasks) {
            for (TaskImpl<?> task : tasks) {
                task.result.cancel(true);
            }
        }
    }

    static class TaskImpl<T> {
        private final Callable<? extends T> task;
        private final CompletableFuture<T> result = new CompletableFuture<>();

        private float delay;

        public TaskImpl(float delay, Callable<? extends T> task) {
            this.delay = delay;
            this.task = task;
        }

        private void run() {
            try {
                result.complete(task.call());
            } catch (Exception t) {
                result.completeExceptionally(t);
            }
        }
    }
}
