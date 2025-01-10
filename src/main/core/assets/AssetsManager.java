package core.assets;

import core.g2d.Texture;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class AssetsManager {

    private final String rootPath, assetsDir;

    private final Map<String, Texture> textures = new HashMap<>();
    private final boolean exploded;

    public AssetsManager(boolean exploded) {
        this.exploded = exploded;
        this.rootPath = normalizePath(Path.of("").toAbsolutePath().toString());
        this.assetsDir = exploded ? rootPath + "/src/assets/" : "";
    }

    public String assetsDir(String path) {
        return assetsDir + normalizePath(path);
    }

    public String pathTo(String path) {
        return rootPath + "/" + normalizePath(path);
    }

    public static String normalizePath(String path) {
        return path == null ? null : path.replace('\\', '/').replaceAll("//", "/");
    }

    public Texture getTextureByPath(String path) {
        Texture tex = textures.get(path);

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

    public InputStream resourceStream(String path) throws IOException {
        if (exploded) {
            return Files.newInputStream(computePath(path));
        } else {
            return AssetsManager.class.getModule().getResourceAsStream(path);
        }
    }

    private static Path computePath(String path) {
        Path p = Path.of(path);
        if (!Files.exists(p)) {
            p = Path.of(path.replace("src/assets", "src/assets-gen"));
        }
        return p;
    }

    public Reader resourceReader(String path) throws IOException {
        if (exploded) {
            return Files.newBufferedReader(computePath(path), StandardCharsets.UTF_8);
        } else {
            InputStream in = resourceStream(path);
            if (in == null) {
                return null;
            }
            return new InputStreamReader(in, StandardCharsets.UTF_8);
        }
    }

    public String readString(String path) throws IOException {
        if (exploded) {
            return Files.readString(computePath(path), StandardCharsets.UTF_8);
        }
        try (InputStream reader = resourceStream(path)) {
            if (reader == null) {
                return null;
            }
            byte[] all = reader.readAllBytes();
            return new String(all, StandardCharsets.UTF_8);
        }
    }
}
