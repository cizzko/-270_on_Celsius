package core.g2d;

import core.math.Mat3;
import core.util.Disposable;

import static org.lwjgl.opengl.GL40.*;

public final class UniformBufferObject implements Disposable {
    private final int id;

    public UniformBufferObject() {
        this.id = glGenBuffers();
    }

    public void bind() {
        glBindBuffer(GL_UNIFORM_BUFFER, id);
    }

    public void upload(Mat3 mat3) {
        bind();
    }

    @Override
    public void close() {
        glDeleteBuffers(id);
    }
}
