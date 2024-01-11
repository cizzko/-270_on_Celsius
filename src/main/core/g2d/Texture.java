package core.g2d;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static core.assets.TextureLoader.BufferedImageEncoder;
import static core.assets.TextureLoader.readImage;
import static org.lwjgl.opengl.GL11.*;

public final class Texture implements Drawable {
    final int glHandle;

    private final int glTarget; // TODO возможно и не нужно, но пускай побудет
    private final int width, height;
    private final float u, v, u2, v2;

    private Texture(int glHandle, int glTarget,
                    int width, int height,
                    float u, float v, float u2, float v2) {
        this.glHandle = glHandle;
        this.glTarget = glTarget;
        this.width = width;
        this.height = height;
        this.u = u;
        this.v = v;
        this.u2 = u2;
        this.v2 = v2;
    }

    public static Texture load(BufferedImage bufferedImage, int glTarget, float u, float v, float u2, float v2) throws IOException {
        var image = readImage(bufferedImage);

        int glHandle = glGenTextures();

        glBindTexture(glTarget, glHandle);
        glTexParameteri(glTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(glTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(glTarget, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(glTarget, GL_TEXTURE_WRAP_T, GL_CLAMP);

        int w = image.width();
        int h = image.height();
        glTexImage2D(glTarget, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, image.data());

        glBindTexture(glTarget, 0);
        return new Texture(glHandle, glTarget, w, h, u, v, u2, v2);
    }

    public static Texture load(String path) throws IOException {
        return load(path, GL_TEXTURE_2D, 0, 0, 1, 1);
    }

    public static Texture load(String path, int glTarget, float u, float v, float u2, float v2) throws IOException {
        return load(BufferedImageEncoder(path), glTarget, u, v, u2, v2);
    }

    public int glTarget() {
        return glTarget;
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
}
