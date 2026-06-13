package core.util;

import it.unimi.dsi.fastutil.ints.IntIntBiConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.concurrent.*;

/// Класс для разделения чанковых задач в [ForkJoinPool]
public final class BatchScope implements Disposable, Executor {
    private final ForkJoinPool pool;
    private final ObjectArrayList<ForkJoinTask<?>> futures;

    private final int workSize;
    private final int poolSize;

    public BatchScope(ForkJoinPool pool, int workSize) {
        this(pool, pool.getParallelism(), workSize);
    }

    public BatchScope(ForkJoinPool pool, int poolSize, int workSize) {
        this.pool = pool;
        this.futures = new ObjectArrayList<>(poolSize);
        this.poolSize = poolSize;
        this.workSize = workSize;
    }

    /// @param lo Базовое смещение координаты слева
    /// @param hi Правая граница для задач
    /// @param task Шаблон задачи который будет применен при разделении `workSize`
    public BatchScope submit(int lo, int hi, IntIntBiConsumer task) {
        if (lo >= hi) return this;
        int length = hi - lo;
        int targetChunkSize = Math.max(1, length / (poolSize * 4));

        pool.invoke(new CountedChunkTask(null, lo, hi, targetChunkSize, task));
        return this;
    }

    public BatchScope submit(IntIntBiConsumer task) {
        return submit(0, workSize, task);
    }

    /// Заблокироваться и ожидать выполнения всех задач
    /// Этот метод собирает все исключения задач через [Throwable#addSuppressed(java.lang.Throwable)]
    /// и выбрасывает в конце
    /// После выполнения этого метода класс можно переиспользовать
    public void awaitAll() {
        close();
    }

    public void close() {
        var tasks = futures;
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
                FutureUtil.sneakyThrow(e);
        } catch (Throwable t) {
            for (var e : tasks)
                e.cancel(true);
            throw t;
        } finally {
            tasks.clear();
        }
    }

    public void execute(Runnable task) {
        futures.add(pool.submit(task));
    }

    static final class FlatChunkTask extends RecursiveAction {
        final IntIntBiConsumer task;
        final int chunkBegin, chunkEnd;

        FlatChunkTask(IntIntBiConsumer task, int chunkBegin, int chunkEnd) {
            this.task = task;
            this.chunkBegin = chunkBegin;
            this.chunkEnd = chunkEnd;
        }

        protected void compute() {
            task.accept(chunkBegin, chunkEnd);
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

                new CountedChunkTask(this, l, mid, thresh, task).fork();
                l = mid;
            }

            task.accept(l, h);
            propagateCompletion();
        }
    }

    static final class SplitTask extends RecursiveAction {
        final IntIntBiConsumer task;
        final int start, end;
        final int threshold;

        SplitTask(IntIntBiConsumer task, int start, int end, int threshold) {
            this.task = task;
            this.start = start;
            this.end = end;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            if ((end - start) <= threshold) {
                task.accept(start, end);
                return;
            }

            int mid = (start + end) >>> 1;
            SplitTask left = new SplitTask(task, start, mid, threshold);
            SplitTask right = new SplitTask(task, mid, end, threshold);

            invokeAll(left, right);
        }
    }
}
