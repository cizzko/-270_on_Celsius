package core.assets;

import core.util.TypeUtil;

import java.nio.file.Path;

/// Загрузчик ассета конкретного типа.
/// Поскольку загрузка ассетов это модель рекурсивного fork-join, то
/// менеджер полностью опирается на это для эффективной загрузки.
/// Типичный цикл загрузки ассета можно представить как:
/// ```java
/// handler.loadAsync(res, name, params, state); // fork
/// invokeAll(forkedTasks);                      // join
/// // Обязательно на главном потоке чтобы
/// // упростить работу с внутренними структурами
/// T result = handler.loadSync(res, name, params, state);
/// ```
public abstract class AssetHandler<T, P, S> {

    protected final Class<T> type;
    protected final String dirName;
    protected Path dir;

    protected AssetHandler(Class<T> type, String dirName) {
        this.type = type;
        this.dirName = dirName;
    }

    void setDir(Path dir) {
        this.dir = dir;
    }

    public Class<T> type() {
        return type;
    }

    public abstract void release(AssetReleaser rel, T asset);

    public abstract void loadAsync(AssetResolver res, String name, P params, S state);

    public abstract T loadSync(AssetResolver res, String name, P params, S state) throws Exception;

    protected abstract P createParams();

    protected abstract S createState();

    @Override
    public String toString() {
        return TypeUtil.canonicalNameOrParent(getClass()) + "<" + TypeUtil.canonicalNameOrParent(type) + ">" + "(dir='" + dirName + "')";
    }
}
