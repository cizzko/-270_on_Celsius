package core;

import core.EventHandling.Logging.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class TaskScheduler {
    private final ArrayList<TaskImpl<?>> tasks = new ArrayList<>(); // synchronized

    public void post(Runnable task, float delay) {
        post(() -> {
            task.run();
            return null;
        }, delay);
    }

    public <T> Supplier<? extends T> post(Callable<? extends T> task, float delay) {
        Objects.requireNonNull(task);

        var future = new TaskImpl<>(delay, task);
        synchronized (tasks) {
            tasks.add(future);
        }
        return future;
    }

    public <T> Supplier<? extends T> post(Callable<? extends T> task) {
        return post(task, 0);
    }

    public void post(Runnable task) {
        post(() -> {
            task.run();
            return null;
        }, 0);
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
                try {
                    task.run();
                } catch (Exception t) {
                    Logger.printException("Failed to execute task: " + task.task, t);
                } finally {
                    handled.add(task);
                }
            }
        }
        if (!handled.isEmpty()) {
            synchronized (tasks) {
                tasks.removeAll(handled);
            }
        }
    }

    static class TaskImpl<T> implements Supplier<T> {
        private float delay;

        private final Callable<? extends T> task;

        private record Result<T>(T value, Exception exception) {}

        private volatile Result<T> result;

        public TaskImpl(float delay, Callable<? extends T> task) {
            this.delay = delay;
            this.task = task;
        }

        private void run() {
            Result<T> res;
            try {
                res = new Result<>(task.call(), null);
            } catch (Exception t) {
                res = new Result<>(null, t);
            }
            result = res;
        }

        @Override
        public T get() {
            var res = result;
            if (res == null) {
                throw new IllegalStateException("Task not executed yet");
            }
            if (res.exception != null) {
                throw new IllegalStateException(res.exception);
            }

            return res.value;
        }
    }
}
