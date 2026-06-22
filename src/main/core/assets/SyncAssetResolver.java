package core.assets;

import core.Global;
import core.util.FutureUtil;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

final class SyncAssetResolver<T, P, S>
        implements BaseAssetResolver {

    final AssetHandler<T, P, S> loader;
    final String name;
    final P params;
    final S state;

    final AssetsManager.Asset<T> desc;
    final ArrayList<AssetsManager.Asset<?>> depends = new ArrayList<>();
    final ArrayList<Future<?>> tasks = new ArrayList<>();

    SyncAssetResolver(AssetHandler<T, P, S> loader, String name, P params, S state) {
        this.desc = new AssetsManager.Asset<>(loader.type(), name);
        this.loader = loader;
        this.name = name;
        this.params = params;
        this.state = state;
    }

    private void checkIfFailed() {
        Throwable exceptionKolbasa = null;
        for (var fut : tasks) {
            if (fut.state() == Future.State.FAILED) {
                var cause = fut.exceptionNow();
                if (exceptionKolbasa == null) {
                    exceptionKolbasa = cause;
                } else {
                    exceptionKolbasa.addSuppressed(cause);
                }
            }
        }
        if (exceptionKolbasa != null) {
            tasks.clear();
            FutureUtil.uncheckedThrow(exceptionKolbasa);
        }
    }

    @Override
    public <T2> Future<T2> fork(Callable<T2> action) {
        var task = Global.scheduler.submit(action);
        tasks.add(task);
        return task;
    }

    @Override
    public Future<Void> fork(Runnable action) {
        var task = Global.scheduler.submit(action);
        tasks.add(task);
        return task;
    }

    @Override
    public <R, P2, S2> Future<R> load(Class<? extends AssetHandler<R, P2, S2>> type, String name, Consumer<? super P2> paramsModifier) {
        return Global.assets.loadInternalByHandler(this, type, name, loadType(), paramsModifier);
    }

    @Override
    public <R, P2> Future<R> load(Class<R> type, String name, AssetsManager.LoadType loadType,
                                  Consumer<? super P2> paramsModifier) {
        if (loadType != AssetsManager.LoadType.SYNC) {
            throw new IllegalArgumentException("Synchronous mode");
        }

        return Global.assets.loadSyncInternal(this, type, name, paramsModifier);
    }

    public CompletableFuture<T> load() {
        return Global.scheduler.submit(() -> {
            loader.loadAsync(this, name, params, state);
            checkIfFailed();
            desc.value = loader.loadSync(this, name, params, state);

            desc.dependencies = depends.isEmpty() ? null : depends.toArray(new AssetsManager.Asset[0]);
            Global.assets.setLoaded(loader.type(), name, desc);
            return desc.value;
        });
    }

    @Override
    public AssetsManager.LoadType loadType() {
        return AssetsManager.LoadType.SYNC;
    }

    @Override
    public ArrayList<AssetsManager.Asset<?>> depends() {
        return depends;
    }
}
