package core.g2d;

import core.graphic.BitMap;
import core.util.Disposable;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL46.*;

public final class Texture implements Drawable, Disposable {
    public static final int MAX_ID = 1 << 16; // TODO сделать безнаковым

    final int target;
    final short id;

    private final int width, height;

    private Texture(int target, short id, int width, int height) {
        this.target = target;
        this.id = id;
        this.width = width;
        this.height = height;
    }

    static short genId(int target) {
        int i = OpenGL.createTextures(target);
        if (i >= MAX_ID) {
            throw new IllegalStateException("Limit of textures exceeded");
        }
        return (short)i;
    }

    static Texture load(BitMap img, int target,
                        int minFilter, int magFilter,
                        int wrapS, int wrapT) {
        short id = genId(target);

        OpenGL.bindTexture(target, id);

        OpenGL.textureParameteri(target, id, GL_TEXTURE_MIN_FILTER, minFilter);
        OpenGL.textureParameteri(target, id, GL_TEXTURE_MAG_FILTER, magFilter);
        OpenGL.textureParameteri(target, id, GL_TEXTURE_WRAP_S, wrapS);
        OpenGL.textureParameteri(target, id, GL_TEXTURE_WRAP_T, wrapT);

        int w = img.width();
        int h = img.height();
        try (img) {
            OpenGL.texStorage2D(target, id, 1, GL_RGBA8, w, h);
            OpenGL.texSubImage2D(target, id, 0, 0, 0, w, h, img.glFormat(), img.glType(), img.data());
        }

        OpenGL.bindTexture(target, 0);

        var tex = new Texture(target, id, w, h);
        ResourceCache.texturesById.put(id, tex);
        OpenGL.saveHandle(id);
        return tex;
    }

    public void setParameteri(int pname, int param) {
        OpenGL.bindTexture(target, id);
        OpenGL.textureParameteri(target, id, pname, param);
        OpenGL.bindTexture(target, 0);
    }

    public short id() { return id; }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public float u()  { return BytePack.toB16(0f); }
    public float v()  { return BytePack.toB16(0f); }
    public float u2() { return BytePack.toB16(1); }
    public float v2() { return BytePack.toB16(1); }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Texture texture && id == texture.id;
    }

    @Override
    public int hashCode() {
        return Short.toUnsignedInt(id);
    }

    public String toString() {
        return "Texture{" + "id=" + id + ", w=" + width + ", h=" + height + '}';
    }

    public void close() {
        ResourceCache.texturesById.remove(id);
        OpenGL.deleteHandle(id);
        glDeleteTextures(id);
    }
}
