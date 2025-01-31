package core.assets;

import java.nio.file.Path;

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

    public abstract T loadSync(String name, P params, S state) throws Exception;

    protected abstract P createParams();

    protected abstract S createState();

    @Override
    public String toString() {
        return getClass().getCanonicalName() + "<" + type.getCanonicalName() + ">" + "(dir='" + dirName + "')";
    }
}
