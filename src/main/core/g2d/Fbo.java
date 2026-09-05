package core.g2d;

import core.util.Disposable;
import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL46.*;

public final class Fbo implements Disposable {

    private final int fboId;
    private final int colorTex;
    private int width, height;

    public Fbo(int width, int height) {
        fboId = glGenFramebuffers();
        colorTex = glGenTextures();
        resize(width, height);
        OpenGL.saveHandle(colorTex);

        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTex, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int colorTex() {
        return colorTex;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void resize(int w, int h) {
        width = w;
        height = h;
        glBindTexture(GL_TEXTURE_2D, colorTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glViewport(0, 0, width, height);
        glColorMask(true, true, true, true);
        glDisable(GL_SCISSOR_TEST);
        glClearColor(206f / 255f, 246f / 255f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void close() {
        OpenGL.deleteHandle(colorTex);
        glDeleteTextures(colorTex);
        glDeleteFramebuffers(fboId);
    }
}