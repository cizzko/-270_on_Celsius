package core.g2d;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public final class ResourceCache {
    private ResourceCache() {}

    public static final Byte2ObjectOpenHashMap<Shader> shadersById    = new Byte2ObjectOpenHashMap<>();
    public static final Short2ObjectOpenHashMap<Texture> texturesById = new Short2ObjectOpenHashMap<>();
}
