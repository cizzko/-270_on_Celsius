package core.util;

import it.unimi.dsi.fastutil.ints.IntIntBiConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Blocking;

import java.util.concurrent.*;

/// Класс для разделения чанковых задач в [ForkJoinPool]
public final class BatchScope implements Disposable, Executor {
    private static final int DEFAULT_LOAD_FACTOR = 4;

    private final ForkJoinPool pool;
    private final ObjectArrayList<ForkJoinTask<?>> futures;

    private int loadFactor;

    public BatchScope(ForkJoinPool pool) {
        this(pool, DEFAULT_LOAD_FACTOR);
    }

    public BatchScope(ForkJoinPool pool, int loadFactor) {
        this.pool = pool;
        this.futures = new ObjectArrayList<>(pool.getParallelism());
        this.loadFactor = loadFactor;
    }

    public void setLoadFactor(int loadFactor) {
        this.loadFactor = loadFactor;
    }

    /// @param lo Базовое смещение координаты слева
    /// @param hi Правая граница для задач
    /// @param task Шаблон задачи который будет применен при разделении `workSize`
    @Blocking
    public BatchScope submit(int lo, int hi, IntIntBiConsumer task) {
        if (lo >= hi) return this;
        int size = hi - lo;
        int threshold = Math.max(1, size / (pool.getParallelism() * loadFactor));

        pool.invoke(new CountedChunkTask(null, lo, hi, threshold, task));
        return this;
    }

    public void execute(Runnable task) {
        futures.add(pool.submit(task));
    }

    /// Заблокироваться и ожидать выполнения всех задач
    /// Этот метод собирает все исключения задач через [Throwable#addSuppressed(java.lang.Throwable)]
    /// и выбрасывает в конце
    /// После выполнения этого метода класс можно переиспользовать
    @Blocking
    public void awaitAll() {
        close();
    }

    @Blocking
    public void close() {
        var tasks = futures;
        if (tasks.isEmpty()) {
            return;
        }
        try {
            for (int i = tasks.size() - 1; i >= 0; --i) {
                var task = tasks.get(i);
                task.quietlyJoin();
            }
            Throwable e = null;
            for (int i = 0; i < tasks.size(); i++) {
                var task = tasks.get(i);
                var exc = task.getException();
                if (exc != null) {
                    if (e == null)
                        e = exc;
                    else
                        e.addSuppressed(exc);
                }
            }
            if (e != null)
                FutureUtil.uncheckedThrow(e);
        } catch (Throwable t) {
            for (var e : tasks)
                e.cancel(true);
            throw t;
        } finally {
            tasks.clear();
        }
    }

    static final class CountedChunkTask extends CountedCompleter<Void> {
        final int lo, hi;
        final int threshold;
        final IntIntBiConsumer task;

        CountedChunkTask(CountedChunkTask parent, int lo, int hi, int threshold, IntIntBiConsumer task) {
            super(parent);
            this.lo = lo;
            this.hi = hi;
            this.threshold = threshold;
            this.task = task;
        }

        @Override
        public void compute() {
            int l = this.lo;
            int h = this.hi;
            int thresh = this.threshold;
            var task = this.task;

            while ((h - l) > thresh) {
                int mid = (l + h) >>> 1;

                addToPendingCount(1);

                new CountedChunkTask(this, mid, h, thresh, task).fork();
                h = mid;
            }

            task.accept(l, h);
            propagateCompletion();
        }
    }
}
