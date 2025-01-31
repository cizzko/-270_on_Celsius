package core.assets;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface AssetResolver {

    AssetsManager.LoadType loadType();

    <T> Future<T> fork(Callable<T> callable);
    Future<Void> fork(Runnable runnable);

    default <T> Future<T> load(Class<T> type, String name) {
        return load(type, name, loadType(), null);
    }

    default <T> Future<T> load(Class<T> type, String name, AssetsManager.LoadType loadType) {
        return load(type, name, loadType, null);
    }

    <T, P> Future<T> load(Class<T> type, String name, AssetsManager.LoadType loadType, Consumer<? super P> paramsModifier);
}
