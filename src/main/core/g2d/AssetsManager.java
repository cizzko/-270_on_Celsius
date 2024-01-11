package core.g2d;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AssetsManager {

    private static final String rootPath = normalizePath(Path.of("").toAbsolutePath().toString());

    // По сути, тут можно завернуть Texture и в WeakReference/SoftReference,
    // но придётся позаботиться об удалении текстуры из GL
    private final Map<String, Texture> textures;

    public AssetsManager() {
        textures = new HashMap<>();
    }

    public String assetsDir(String path) {
        return rootPath + "/src/assets/" + normalizePath(path);
    }

    public String pathTo(String path) {
        return rootPath + normalizePath(path);
    }

    public static String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    public Texture getTextureByPath(String path) {
        var tex = textures.get(path);
        if (tex == null) {
            try {
                tex = Texture.load(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            textures.put(path, tex);
        }
        return tex;
    }
}
