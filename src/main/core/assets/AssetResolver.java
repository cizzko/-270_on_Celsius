package core.assets;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public sealed interface AssetResolver permits BaseAssetResolver {

    AssetsManager.LoadType loadType();

    <T> Future<T> fork(Callable<T> action);
    Future<Void> fork(Runnable action);

    <T, P, S> Future<T> load(Class<? extends AssetHandler<T, P, S>> type, String name, Consumer<? super P> paramsModifier);

    <T, P> Future<T> load(Class<T> type, String name, AssetsManager.LoadType loadType, Consumer<? super P> paramsModifier);

    <T> Future<T> load(Class<T> type, String name);

    InputStream openStream(String name) throws IOException;
    InputStream openStreamInDir(String name, String... extensions) throws IOException;
}
