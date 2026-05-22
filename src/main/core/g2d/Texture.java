package core.g2d;

import core.graphic.BitMap;
import core.util.Disposable;

import java.awt.image.BufferedImage;

import static core.graphic.TextureLoader.decodeImage;
import static org.lwjgl.opengl.GL46.*;

public final class Texture implements Drawable, Disposable {
    public static final int MAX_ID = 1 << 16; // TODO сделать безнаковым

    final short glHandle;

    private final int width, height;
    private final short u, v, u2, v2;

    Texture(short glHandle, int width, int height, short u, short v, short u2, short v2) {
        this.glHandle = glHandle;
        this.width = width;
        this.height = height;
        this.u = BytePack.toB16(u);
        this.v = BytePack.toB16(v);
        this.u2 = BytePack.toB16(u2);
        this.v2 = BytePack.toB16(v2);
    }

    static Texture load(BufferedImage bufferedImage,
                        int glTarget,
                        int glClamp,
                        short u, short v, short u2, short v2) {
        var image = decodeImage(bufferedImage);
        return load(image, glTarget, glClamp, u, v, u2, v2);
    }

    static short genId() {
        int i = glGenTextures();
        if (i >= MAX_ID) {
            throw new IllegalStateException("Limit of textures exceeded");
        }
        return (short)i;
    }

    static Texture load(BitMap img,
                        int glTarget, int glClamp,
                        short u, short v, short u2, short v2) {
        short glHandle = genId();

        glBindTexture(glTarget, glHandle);
        glTexParameteri(glTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(glTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(glTarget, GL_TEXTURE_WRAP_S, glClamp);
        glTexParameteri(glTarget, GL_TEXTURE_WRAP_T, glClamp);

        int w = img.width();
        int h = img.height();
        try (img) {
            glTexImage2D(glTarget, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, img.data());
        }
        glBindTexture(glTarget, 0);
        var tex = new Texture(glHandle, w, h, u, v, u2, v2);
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

    @Override
    public float u() {
        return u;
    }

    @Override
    public float v() {
        return v;
    }

    @Override
    public float u2() {
        return u2;
    }

    @Override
    public float v2() {
        return v2;
    }

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
