package core.assets;

import core.Global;

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

    SyncAssetResolver(AssetHandler<T, P, S> loader, String name, P params, S state) {
        this.desc = new AssetsManager.Asset<>(loader.type(), name);
        this.loader = loader;
        this.name = name;
        this.params = params;
        this.state = state;
    }

    @Override
    public <T2> Future<T2> fork(Callable<T2> callable) {
        return Global.scheduler.execute(callable);
    }

    @Override
    public Future<Void> fork(Runnable runnable) {
        return Global.scheduler.execute(runnable);
    }

    @Override
    public <T2, P2> Future<T2> load(Class<T2> type, String name, AssetsManager.LoadType loadType,
                                    Consumer<? super P2> paramsModifier) {
        if (loadType != AssetsManager.LoadType.SYNC) {
            throw new IllegalArgumentException("Synchronous mode");
        }

        return Global.assets.loadSyncInternal(this, type, name, paramsModifier);
    }

    public CompletableFuture<T> load() {
        return Global.scheduler.execute(() -> {
            loader.loadAsync(this, name, params, state);
            desc.value = loader.loadSync(name, params, state);
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
