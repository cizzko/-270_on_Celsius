package core.g2d;

import core.graphic.BitMap;
import core.util.Disposable;

import static org.lwjgl.opengl.GL46.*;

public final class Texture implements Drawable, Disposable {
    public static final int MAX_ID = 1 << 16; // TODO сделать безнаковым

    final short glHandle;

    private final int width, height;

    private Texture(short glHandle, int width, int height) {
        this.glHandle = glHandle;
        this.width = width;
        this.height = height;
    }

    static short genId() {
        int i = glGenTextures();
        if (i >= MAX_ID) {
            throw new IllegalStateException("Limit of textures exceeded");
        }
        return (short)i;
    }

    static Texture load(BitMap img, int target,
                        int minFilter, int magFilter,
                        int wrapS, int wrapT) {
        short glHandle = genId();

        glBindTexture(target, glHandle);
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, magFilter);
        glTexParameteri(target, GL_TEXTURE_WRAP_S, wrapS);
        glTexParameteri(target, GL_TEXTURE_WRAP_T, wrapT);

        int w = img.width();
        int h = img.height();
        try (img) {
            glTexImage2D(target, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, img.data());
        }
        glBindTexture(target, 0);
        var tex = new Texture(glHandle, w, h);
        ResourceCache.texturesById.put(glHandle, tex);
        return tex;
    }

    @Override
    public short id() { return glHandle; }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }


    public float u()  { return BytePack.toB16(0f); }
    public float v()  { return BytePack.toB16(0f); }
    public float u2() { return BytePack.toB16(1); }
    public float v2() { return BytePack.toB16(1); }

    @Override
    public String toString() {
        return "Texture{" + "id=" + glHandle + ", w=" + width + ", h=" + height + '}';
    }

    @Override
    public void close() {
        ResourceCache.texturesById.remove(glHandle);
        glDeleteTextures(glHandle);
    }
}
