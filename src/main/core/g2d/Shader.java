package core.g2d;

import core.Utils.Disposable;
import core.math.Mat3;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL46.*;

public final class Shader implements Disposable {
    private int glHandle;

    private int vertexSize;
    private float[] mat4adapt;

    private final Map<String, ShaderAttribute> attributes = Map.of(); // new LinkedHashMap<>();

    public Shader(int program) {
        this.glHandle = program;

        // TODO всё таки сделать автоматическую настройку вершинных данных
        //  но при этом учесть оптимизированную запись цвета как 1 float и т.п., т.д.
        // try (var stack = MemoryStack.stackPush()) {
        //     int numAttrs = glGetProgrami(glHandle, GL_ACTIVE_ATTRIBUTES);
        //     for (int i = 0; i < numAttrs; i++) {
        //         IntBuffer size = stack.mallocInt(1);
        //         IntBuffer type = stack.mallocInt(1);
        //         var name = glGetActiveAttrib(program, i, size, type);
        //
        //         int loc = glGetAttribLocation(glHandle, name);
        //         ShaderAttribute attr = new ShaderAttribute(name, size.get(0), type.get(0), loc);
        //         vertexSize += attr.size();
        //
        //         attributes.put(name, attr);
        //     }
        // }
    }

    public int vertexSize() {
        return vertexSize;
    }

    public Collection<ShaderAttribute> attributes() {
        return attributes.values();
    }

    public record ShaderAttribute(String name, int components, int type, int location) {
        public int size() {
            return switch (type) {
                case GL_FLOAT_VEC4 -> 4 * components;
                case GL_FLOAT_VEC2 -> 2 * components;
                default -> throw new IllegalArgumentException(Integer.toHexString(type));
            };
        }

        public boolean normalized() {
            return false; // TODO нужно ли?
        }

        @Override
        public String toString() {
            return "ShaderAttribute{" +
                    "name='" + name + '\'' +
                    ", components=" + components +
                    ", type=" + type +
                    ", location=" + location +
                    ", normalized=" + normalized() +
                    ", size=" + size() +
                    '}';
        }

        public int vertexType() {
            return GL_FLOAT;
        }
    }

    public static Shader load(String basePath) throws IOException {
        String vertSource = Files.readString(Path.of(basePath + ".vert"));
        String fragSource = Files.readString(Path.of(basePath + ".frag"));
        return load(vertSource, fragSource);
    }

    public static Shader load(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        var program = glCreateProgram();
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
        var buffer = mat4adapt;
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
