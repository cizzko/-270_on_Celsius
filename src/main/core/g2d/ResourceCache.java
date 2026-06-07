package core.g2d;

import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public final class ResourceCache {
    private ResourceCache() {}

    public static final Shader[] shadersById = new Shader[Shader.MAX_ID];
    public static final Short2ObjectOpenHashMap<Texture> texturesById = new Short2ObjectOpenHashMap<>();
}
