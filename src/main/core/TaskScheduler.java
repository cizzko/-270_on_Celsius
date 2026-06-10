package core;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

public final class TaskScheduler {
    public static final TaskImpl<?>[] ZERO = new TaskImpl[0];

    // TODO тут нужна MPSC очередь, желательно фиксированного размера
    private final /* synchronized */ TaskQueue tasks = new TaskQueue();
    private TaskImpl<?>[] guardedQueue = ZERO;
    private volatile boolean running = true;

    static final class TaskQueue { // аааааа, меня вынудили
        TaskImpl<?>[] a = ZERO;
        int size;

        void grow(int capacity) {
            if (capacity <= a.length) return;
            if (a != ZERO)
                capacity = Math.clamp((long) a.length + (a.length >> 1), capacity, it.unimi.dsi.fastutil.Arrays.MAX_ARRAY_SIZE);
            else if (capacity < ObjectArrayList.DEFAULT_INITIAL_CAPACITY)
                capacity = ObjectArrayList.DEFAULT_INITIAL_CAPACITY;
            a = Arrays.copyOf(a, capacity);
        }

        void add(TaskImpl<?> k) {
            grow(size + 1);
            a[size++] = k;
        }

        void addAllToFront(TaskImpl<?>[] sourceArray, int countToCopy) {
            if (countToCopy <= 0) return;

            int currentSize = this.size;
            var arr = this.a;
            int newSize = currentSize + countToCopy;

            if (newSize > arr.length) {
                int newCapacity = Math.max(arr.length * 2, newSize);
                var newElements = new TaskImpl<?>[newCapacity];

                System.arraycopy(arr, 0, newElements, countToCopy, currentSize);
                this.a = newElements;
            } else {
                System.arraycopy(arr, 0, arr, countToCopy, currentSize);
            }

            System.arraycopy(sourceArray, 0, this.a, 0, countToCopy);
            this.size = newSize;
        }
    }

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
        var tq = tasks;
        int count;
        var wq = guardedQueue;
        synchronized (tq) {
            count = tq.size;
            if (count == 0) {
                return;
            }

            var tqArray = tq.a;
            if (wq.length < count) {
                guardedQueue = wq = Arrays.copyOf(tqArray, count);
            } else {
                System.arraycopy(tqArray, 0, wq, 0, count);
            }
            for (int i = 0; i < count; i++)
                tqArray[i] = null;
            tq.size = 0;
        }

        for (int i = 0; i < count; i++) {
            var task = wq[i];
            task.delay -= Time.delta;

            if (task.delay <= 0) {
                wq[i] = null;
                task.run();
            }
        }
        int aliveTasks = 0;
        for (int i = 0; i < count; i++) {
            if (wq[i] != null) {
                if (i != aliveTasks) {
                    wq[aliveTasks] = wq[i];
                    wq[i] = null;
                }
                aliveTasks++;
            }
        }

        if (aliveTasks > 0) {
            synchronized (tq) {
                tq.addAllToFront(wq, aliveTasks);
            }
            for (int i = 0; i < aliveTasks; i++)
                wq[i] = null;
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

        var tq = tasks;
        synchronized (tasks) {
            for (int i = 0; i < tq.size; i++) {
                tq.a[i].result.cancel(true);
                tq.a[i] = null;
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
