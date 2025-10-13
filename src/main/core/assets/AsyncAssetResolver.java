package core.assets;

import core.Global;
import org.lwjgl.system.NativeResource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

final class AsyncAssetResolver<T, P, S>
        extends RecursiveTask<T>
        implements BaseAssetResolver {

    final AsyncAssetResolver<?, ?, ?> parent;
    final AssetHandler<T, P, S> loader;
    final String name;
    final P params;
    final S state;

    final AssetsManager.Asset<T> desc;
    final ArrayList<AssetsManager.Asset<?>> depends = new ArrayList<>();
    final ArrayList<ForkJoinTask<?>> tasks = new ArrayList<>();

    public AsyncAssetResolver(AsyncAssetResolver<?, ?, ?> parent,
                              AssetHandler<T, P, S> loader, String name, P params, S state) {
        this.parent = parent;
        this.desc = new AssetsManager.Asset<>(loader.type(), name);
        this.loader = loader;
        this.name = name;
        this.params = params;
        this.state = state;
    }

    @Override
    public AssetsManager.LoadType loadType() {
        return AssetsManager.LoadType.ASYNC;
    }

    @Override
    public List<AssetsManager.Asset<?>> depends() {
        return depends;
    }

    @Override
    public <A> Future<A> fork(Callable<A> callable) {
        var task = new DependencyTask<>(this, callable);
        tasks.add(task);
        return task;
    }

    @Override
    public Future<Void> fork(Runnable runnable) {
        return fork(() -> {
            runnable.run(); // лень делать отдельный тип
            return null;
        });
    }

    @Override
    public <T2, P2> Future<T2> load(Class<T2> type, String name, AssetsManager.LoadType loadType,
                                    Consumer<? super P2> paramsModifier) {
        return Global.assets.loadInternal(this, type, name, loadType, paramsModifier);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            parent.cancel(false); // AsyncAssetResolver не должен прерываться
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable> void rethrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    protected T compute() {
        // TODO разобраться с обёртыванием исключений в RuntimeException

        loader.loadAsync(this, name, params, state);

        // Специализированный вариант invokeAll(), который работает по принципу fail-fast
        // Загрузка ассета, если есть какие-либо исключения, должна прерываться и клиент должен сам это решать
        // Да, в большинстве случаев исключение тут это именно что _опечатки_ или прочие ошибки в ядре игры
        int last = tasks.size() - 1;
        Throwable anyExc = null;
        for (int i = last; i >= 0; --i) {
            ForkJoinTask<?> t = tasks.get(i);
            if (isCancelled()) { // нас мог оповестил дочерний таск
                break;
            }

            if (i == 0) { // запустим последний таск на этом потоке, всё равно предстоит ждать
                t.quietlyInvoke();
                var exc = t.getException();
                if (exc != null) {
                    anyExc = exc;
                }
            } else {
                t.fork();
            }
        }

        for (int i = 1; i <= last; ++i) {
            ForkJoinTask<?> t = tasks.get(i);
            if (anyExc != null || isCancelled()) {
                // первый таск в очереди упал с исключением, отменяем все остальные
                t.cancel(true);
            } else {
                t.quietlyJoin();
            }
            var exc = t.getException();
            // либо пользовательские исключения либо CancellationException
            if (anyExc == null) {
                anyExc = exc;
            } else {
                anyExc.addSuppressed(exc);
            }
        }

        if (anyExc != null) {
            cleanupTasks();
            rethrow(anyExc);
            return null;
        }

        if (isCancelled()) {
            cleanupTasks();
            return null;
        }

        var syncAction = Global.scheduler.post(() -> {
            T assetInst = loader.loadSync(name, params, state);
            if (assetInst == null) {
                throw new IllegalStateException(
                        loader + " returned null for asset '" +
                                name + "', params=" + params + ", state=" + state);
            }

            desc.value = assetInst;
            desc.dependencies = depends.isEmpty() ? null : depends.toArray(new AssetsManager.Asset[0]);

            Global.assets.setAsyncLoaded(loader.type(), name, desc);

            return assetInst;
        });

        try {
            return syncAction.join();
        } catch (CompletionException | CancellationException e) {
            cleanupTasks();
            rethrow(e.getCause());
            return null;
        }
    }

    private void cleanupTasks() {
        for (ForkJoinTask<?> t : tasks) {
            if (t.state() == State.SUCCESS) {

                // TODO А можем ли мы лучше? Хочется объединить функционал чистки в AssetsManager
                if (t.resultNow() instanceof NativeResource n) {
                    try {
                        n.free();
                    } catch (Exception e) {
                        // TODO а нас интересует это?
                        AssetsManager.log.error("[{}] Failed to release native resource {}", this, n, e);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "AssetLoadTask{name='" + name + "', loader=" + loader + "}";
    }

    static final class DependencyTask<T> extends ForkJoinTask<T> implements RunnableFuture<T> {
        final AsyncAssetResolver<?, ?, ?> parent;
        final Callable<? extends T> callable;

        T result;
        volatile Thread runner;

        DependencyTask(AsyncAssetResolver<?, ?, ?> parent, Callable<? extends T> callable) {
            this.parent = parent;
            this.callable = callable;
        }

        @Override
        public T getRawResult() {
            return result;
        }

        @Override
        public void setRawResult(T v) {
            result = v;
        }

        @Override
        public boolean exec() {
            Thread.interrupted();
            runner = Thread.currentThread();
            try {
                if (!isDone())
                    result = callable.call();
                return true;
            } catch (Exception e) {
                AsyncAssetResolver.rethrow(e);
                return false;
            } finally {
                Thread.interrupted();
                runner = null;
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            Thread t;
            boolean stat = super.cancel(false);
            if (mayInterruptIfRunning && (t = runner) != null) {
                try {
                    t.interrupt();
                } catch (Throwable ignore) {
                }
            }
            if (stat) {
                parent.cancel(false); // Основной таск должен корректно освободить ресурсы
            }

            return stat;
        }

        @Override
        public void run() {
            invoke();
        }
    }
}
