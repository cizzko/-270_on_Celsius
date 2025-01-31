package core.assets;

import core.Global;
import core.g2d.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class AssetsManager {

    // TODO а точно нужна карта? Может хочется сделать полиморфные загрузчики
    //  Вообще, тут возможно состояние гонки, т.к. к этим объектам идёт доступ из других потоков в load()
    final Map<Class<?>, AssetHandler<?, ?, ?>> handlers = new IdentityHashMap<>();
    final Map<Object, Asset<?>> refsByAssets = new HashMap<>();
    final Map<Class<?>, Map<String, ? super Asset<?>>> assets = new IdentityHashMap<>();
    final Path workingDir, assetsDir;

    final ConcurrentHashMap<Class<?>, Map<String, AsyncAssetResolver<?, ?, ?>>> loading = new ConcurrentHashMap<>();
    final ForkJoinPool executor = ForkJoinPool.commonPool();
    final AssetReleaser releaser = this::releaseInternal;

    public AssetsManager(boolean exploded) {
        this.workingDir = Path.of(System.getProperty("user.dir"));
        if (exploded) {
            this.assetsDir = workingDir.resolve("src").resolve("assets");
        } else {
            var module = AssetsManager.class.getModule();
            this.assetsDir = Path.of(URI.create("jrt:/" + module.getName()));
        }

        register(new ShaderHandler());
        register(new FontHandler());
        register(new TextureHandler());
        register(new AtlasHandler());
    }

    public void register(AssetHandler<?, ?, ?> loader) {
        loader.setDir(assetsDir.resolve(loader.dirName));
        handlers.put(loader.type(), loader);
    }

    public Path workingDir() {
        return workingDir;
    }

    public Path assetsDir() {
        return assetsDir;
    }

    @Deprecated // TODO не использовать
    public static String normalizePath(String path) {
        return path == null ? null : path.replace('\\', '/');
    }

    public InputStream resourceStream(String path) throws IOException {
        Path resolve = assetsDir.resolve(path);
        if (Files.isRegularFile(resolve)) {
            return Files.newInputStream(resolve);
        }
        return null;
    }

    public Reader resourceReader(String path) throws IOException {
        Path resolve = assetsDir.resolve(path);
        if (Files.isRegularFile(resolve)) {
            return Files.newBufferedReader(resolve, StandardCharsets.UTF_8);
        }
        return null;
    }

    public void unload(Class<?> type, String name) {
        Global.app.ensureMainThread(); // TODO может менять поток?

        var assetMap = getAssets(type);
        if (assetMap == null) {
            return;
        }
        var assetRef = assetMap.get(name);
        if (assetRef == null) {
            return;
        }
        unloadInternal(assetRef);
    }

    public void unload(Object asset) {
        Global.app.ensureMainThread(); // TODO может менять поток?

        var assetRef = refsByAssets.get(asset);
        if (assetRef == null) {
            return;
        }
        unloadInternal(assetRef);
    }

    public void debug() {
        System.out.println();
        for (var e : assets.entrySet()) {
            String resTypeName = e.getKey().getCanonicalName();
            var resMap = e.getValue();
            System.out.println("> resourceType='" + resTypeName + "' (count=" + resMap.size() +")");
            for (var res : resMap.values()) {
                var assetRef = (Asset<?>) res;
                System.out.println("| name='" + assetRef.name + "', refCount=" + assetRef.refCount + ", value=" + assetRef.value);
            }
        }
        System.out.println();
    }

    public void unloadAll() {
        Global.app.ensureMainThread();

        executor.shutdown();

        for (Map<String, AsyncAssetResolver<?, ?, ?>> tasksMap : loading.values()) {
            for (AsyncAssetResolver<?, ?, ?> task : tasksMap.values()) {
                task.cancel(true);
            }
        }
        loading.clear();

        assets.forEach((type, assetsMap) -> {
            for (Object asset : assetsMap.values()) {
                Asset<?> assetRef = (Asset<?>) asset;
                releaseInternal(assetRef.value);
            }
        });
        assets.clear();
        refsByAssets.clear();
        handlers.clear();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // не имеет значения. мы делаем эту работу на главном потоке
        }
    }

    public enum LoadType {
        ASYNC,
        SYNC
    }

    public <T> Future<T> load(Class<T> type, String name) {
        return load(type, name, LoadType.ASYNC, null);
    }

    public <T> Future<T> load(Class<T> type, String name, LoadType loadType) {
        return load(type, name, loadType, null);
    }

    public <T, P> Future<T> load(Class<T> type, String name, LoadType loadType,
                                 Consumer<? super P> paramsModifier) {
        return loadInternal(null, type, name, loadType, paramsModifier);
    }

    // region Детали реализации

    void unloadInternal(Asset<?> assetRef) {

        Set<Asset<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<Asset<?>> queue = new ArrayDeque<>();
        queue.add(assetRef);
        Asset<?> dep;
        while (!queue.isEmpty()) {
            dep = queue.removeFirst();
            if (dep.refCount.decrementAndGet() == 0) {
                refsByAssets.remove(dep.value);
                assets.get(dep.type).remove(dep.name);
                releaseInternal(dep.value);
                System.out.println("[Assets] Released: " + dep);
            }
            if (visited.add(dep)) {
                if (dep.dependencies != null) {
                    for (Asset<?> dependency : dep.dependencies) {
                        queue.addLast(dependency);
                    }
                }
            }
        }
    }

    <T, P> Future<T> loadSyncInternal(BaseAssetResolver parent,
                                      Class<T> type, String name,
                                      Consumer<? super P> paramsModifier) {
        var loadedAssets = getAssets(type);
        if (loadedAssets != null) {
            var loadedInst = loadedAssets.get(name);
            if (loadedInst != null) {
                incrementRefCount(parent, loadedInst);
                return CompletableFuture.completedFuture(loadedInst.value);
            }
        }

        AssetHandler<T, P, Object> loader = getHandler(type);
        var params = loader.createParams();
        if (paramsModifier != null) {
            paramsModifier.accept(params);
        }

        var state = loader.createState();
        return loadSync((SyncAssetResolver<?, ?, ?>) parent, name, loader, params, state);
    }

    <T, P> Future<T> loadInternal(BaseAssetResolver parent,
                                  Class<T> type, String name, LoadType loadType,
                                  Consumer<? super P> paramsModifier) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(name);
        if (name.isBlank())
            throw new IllegalArgumentException("name cannot be blank");
        Objects.requireNonNull(loadType);

        var loadingMap = getLoadingAssets(type);
        if (loadingMap != null) {
            var future = loadingMap.get(name);
            if (future != null) {
                return future;
            }
        }

        var loadedAssets = getAssets(type);
        if (loadedAssets != null) {
            var loadedInst = loadedAssets.get(name);
            if (loadedInst != null) {
                incrementRefCount(parent, loadedInst);
                return CompletableFuture.completedFuture(loadedInst.value);
            }
        }

        AssetHandler<T, P, Object> loader = getHandler(type);
        var params = loader.createParams();
        if (paramsModifier != null) {
            paramsModifier.accept(params);
        }

        var state = loader.createState();
        if (loadType == LoadType.SYNC && Global.app.isMainThread()) {
            // Это гениально синхронно грузить ресурсы по просьбе из другого потока
            // Хотя бы по той причине, что главный поток будет больше времени заниматься не своим делом
            return loadSync((SyncAssetResolver<?, ?, ?>) parent, name, loader, params, state);
        } else {
            return loadAsync((AsyncAssetResolver<?, ?, ?>) parent, type, name, loader, params, state, loadingMap);
        }
    }

    <T> void releaseInternal(T asset) {
        // TODO что насчёт полиморфизма?
        //  Подумаю, нужно ли переусложнять. Указывать везде тип, который обрабатывает AssetHandler немного неудобно
        @SuppressWarnings("unchecked")
        var type = (Class<T>) asset.getClass();
        AssetHandler<T, ?, ?> handler = getHandler(type);

        handler.release(releaser, asset);
    }

    private void incrementRefCount(BaseAssetResolver parent, Asset<?> loadedInst) {
        if (parent != null) {
            loadedInst.refCount.incrementAndGet();
            parent.depends().add(loadedInst);
        }
    }

    private <T, P> AsyncAssetResolver<T, P, Object> loadAsync(AsyncAssetResolver<?, ?, ?> parent,
                                                              Class<T> type, String name,
                                                              AssetHandler<T, P, Object> loader, P params, Object state,
                                                              Map<String, AsyncAssetResolver<T, ?, ?>> loadingMap) {
        var res = new AsyncAssetResolver<>(parent, loader, name, params, state);
        if (loadingMap == null) {
            var newMap = new ConcurrentHashMap<String, AsyncAssetResolver<?, ?, ?>>();
            newMap.put(name, res);
            loading.putIfAbsent(type, newMap);
        } else {
            loadingMap.put(name, res);
        }

        if (parent != null) {
            incrementRefCount(parent, res.desc);
            parent.tasks.add(res);
        } else {
            executor.submit(res);
        }
        return res;
    }

    private <T, P> Future<T> loadSync(SyncAssetResolver<?, ?, ?> parent, String name,
                                      AssetHandler<T, P, Object> loader,
                                      P params, Object state) {
        var res = new SyncAssetResolver<T, P, Object>(loader, name, params, state);
        incrementRefCount(parent, res.desc);
        return res.load();
    }

    @SuppressWarnings("unchecked")
    <T> Map<String, Asset<T>> getAssets(Class<T> type) {
        return (Map<String, Asset<T>>) (Map<String, ?>) assets.get(type);
    }

    @SuppressWarnings("unchecked")
    <T> Map<String, Asset<T>> getAssetsOrCreate(Class<T> type) {
        return (Map<String, Asset<T>>) (Map<String, ?>) assets.computeIfAbsent(type, k -> new HashMap<String, Asset<?>>());
    }

    @SuppressWarnings("unchecked")
    <T> Map<String, AsyncAssetResolver<T, ?, ?>> getLoadingAssets(Class<T> type) {
        return (Map<String, AsyncAssetResolver<T, ?, ?>>) (Map<String, ?>) loading.get(type);
    }

    @SuppressWarnings("unchecked")
    <T, P, S> AssetHandler<T, P, S> getHandler(Class<T> type) {
        var loader = (AssetHandler<T, P, S>) handlers.get(type);
        if (loader == null) {
            throw new IllegalStateException("Unknown asset type: " + type);
        }
        return loader;
    }

    <T> void setAsyncLoaded(Class<T> type, String name, Asset<T> asset) {
        setLoaded(type, name, asset);

        var loadingMap = getLoadingAssets(type);
        if (loadingMap != null) {
            loadingMap.remove(name);
        }
    }

    <T> void setLoaded(Class<T> type, String name, Asset<T> asset) {
        var typeMap = getAssetsOrCreate(type);
        typeMap.put(name, asset);
        refsByAssets.put(asset.value, asset);
    }

    static final class Asset<T> {
        final Class<T> type;
        final String name;
        final AtomicInteger refCount = new AtomicInteger(1);

        T value;
        Asset<?>[] dependencies;

        Asset(Class<T> type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public String toString() {
            return type.getCanonicalName() + "$" + refCount + "['" + name + "']";
        }
    }

    // endregion
}
