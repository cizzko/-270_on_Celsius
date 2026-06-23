package core.assets;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public sealed interface AssetResolver permits BaseAssetResolver {

    AssetsManager.LoadType loadType();

    <T> Future<T> fork(Callable<T> action);
    Future<Void> fork(Runnable action);

    default <T> Future<T> load(Class<T> type, String name) {
        return load(type, name, loadType(), null);
    }

    <T, P, S> Future<T> load(Class<? extends AssetHandler<T, P, S>> type, String name, Consumer<? super P> paramsModifier);

    default <T> Future<T> load(Class<T> type, String name, AssetsManager.LoadType loadType) {
        return load(type, name, loadType, null);
    }

    <T, P> Future<T> load(Class<T> type, String name, AssetsManager.LoadType loadType, Consumer<? super P> paramsModifier);
}
