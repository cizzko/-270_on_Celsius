package core.g2d;

import core.Utils.Disposable;
import core.math.Mat3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL46.*;

public final class Shader implements Disposable {
    private int glHandle;

    private float[] mat4adapt;

    public Shader(int program) {
        this.glHandle = program;
    }

    public static Shader load(String basePath) throws IOException {
        String vertSource = Files.readString(Path.of(basePath + ".vert"));
        String fragSource = Files.readString(Path.of(basePath + ".frag"));

        return load(vertSource, fragSource);
    }

    public static Shader load(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        int status = glGetProgrami(program, GL_LINK_STATUS);
        if (status != GL_TRUE) {
            String log = glGetProgramInfoLog(program);
            throw new IllegalArgumentException("Failed to link shader:\n" + log);
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return new Shader(program);
    }

    private static int compileShader(int type, String source) {
        int glHandle = glCreateShader(type);
        if (glHandle == 0) {
            return 0; // Ошибка
        }
        glShaderSource(glHandle, source);
        glCompileShader(glHandle);
        int status = glGetShaderi(glHandle, GL_COMPILE_STATUS);
        if (status != GL_TRUE) {
            String log = glGetShaderInfoLog(glHandle);
            glDeleteShader(glHandle);

            String typeStr = switch (type) {
                case GL_VERTEX_SHADER -> "vertex";
                case GL_FRAGMENT_SHADER -> "fragment";
                default -> "unnamed(0x" + Integer.toHexString(type) + ")";
            };
            throw new IllegalArgumentException("Failed to compile " + typeStr + " shader:\n" + log);
        }
        return glHandle;
    }

    public void use() {
        glUseProgram(glHandle);
    }

    public void setUniform(String name, Texture tex) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(tex.glTarget(), tex.glHandle);

        setUniform(name, 0);
    }

    public void setUniform(String name, int val) {
        glUniform1i(uniformLocation(name), val);
    }

    public void setUniformTransforming(String name, Mat3 val) {
        float[] buffer = mat4adapt;
        if (buffer == null) {
            buffer = mat4adapt = new float[16];
        }

        toMat4(val, buffer);
        glUniformMatrix4fv(uniformLocation(name), false, buffer);
    }

    private static void toMat4(Mat3 mat3, float[] res) {

        float[] val = mat3.val;
        res[0]  = val[Mat3.M00];
        res[4]  = val[Mat3.M01];
        res[12] = val[Mat3.M02];

        res[1]  = val[Mat3.M10];
        res[5]  = val[Mat3.M11];
        res[10] = val[Mat3.M22];

        res[13] = val[Mat3.M12];
        res[15] = 1;
    }

    private int uniformLocation(String name) {
        return glGetUniformLocation(glHandle, name);
    }

    @Override
    public boolean isDisposed() {
        return glHandle == 0;
    }

    @Override
    public void close() {
        if (glHandle != 0) {
            glDeleteProgram(glHandle);
            glHandle = 0;
        }
    }
}
