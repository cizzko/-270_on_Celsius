package core.g2d;

import java.awt.image.BufferedImage;

import static core.assets.TextureLoader.decodeImage;
import static org.lwjgl.opengl.GL46.*;

public final class Texture implements Drawable {
    int glHandle;

    private final int width, height;
    private final float u, v, u2, v2;

    Texture(int glHandle, int width, int height, float u, float v, float u2, float v2) {
        this.glHandle = glHandle;
        this.width = width;
        this.height = height;
        this.u = u;
        this.v = v;
        this.u2 = u2;
        this.v2 = v2;
    }

    static Texture load(BufferedImage bufferedImage, int glTarget, float u, float v, float u2, float v2) {
        var image = decodeImage(bufferedImage);
        return load(image, glTarget, u, v, u2, v2);
    }

    static Texture load(BitMap img, int glTarget, float u, float v, float u2, float v2) {
        int glHandle = glGenTextures();

        glBindTexture(glTarget, glHandle);
        glTexParameteri(glTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(glTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(glTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(glTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        int w = img.width();
        int h = img.height();
        try (img) {
            glTexImage2D(glTarget, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, img.data());
        }
        glBindTexture(glTarget, 0);
        return new Texture(glHandle, w, h, u, v, u2, v2);
    }

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
}
