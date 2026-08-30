package core.g2d;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;

public final class ResourceCache {
    private ResourceCache() {}

    public static final Shader[] shadersById = new Shader[Shader.MAX_ID];
    public static final Short2ObjectOpenHashMap<Texture> texturesById = new Short2ObjectOpenHashMap<>();

    private static final ObjectOpenHashSet<VertexFormat> cache = new ObjectOpenHashSet<>();

    public static void dispose() {
        for (var vertexFormat : cache) {
            glDeleteVertexArrays(vertexFormat.vao);
        }
        cache.clear();
    }

    public static void dispose(VertexFormat format) {
        if (cache.remove(format)) {
            if (--format.refCount == 0) {
                glDeleteVertexArrays(format.vao);
            }
        }
    }

    public static VertexFormat intern(VertexFormat vertexFormat) {
        if (vertexFormat.vao != 0)
            throw new IllegalArgumentException();
        var ex = cache.addOrGet(vertexFormat);
        if (ex != null && vertexFormat != ex) {
            ex.refCount++;
            return ex;
        }
        return vertexFormat;
    }
}
