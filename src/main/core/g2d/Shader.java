package core.g2d;

import core.math.Mat3;
import core.util.Disposable;

import java.util.Map;

import static core.math.Mat3.*;
import static org.lwjgl.opengl.GL46.*;

public final class Shader implements Disposable {
    public static final int MAX_ID = 1 << 8;

    public static final String META_EXT = ".meta.json";
    public static final String VERT_EXT = ".vert";
    public static final String FRAG_EXT = ".frag";

    final byte id;
    final String shaderName;

    final VertexFormat vertexFormat;
    final Map<String, Uniform> uniforms;

    Shader(byte id, String shaderName, VertexFormat vertexFormat, Map<String, Uniform> uniforms) {
        this.id = id;
        this.shaderName = shaderName;
        this.vertexFormat = vertexFormat;
        this.uniforms = Map.copyOf(uniforms);

        int uniformCount = glGetProgrami(id, GL_ACTIVE_UNIFORMS);
        for (int i = 0; i < uniformCount; i++) {
            String name = glGetActiveUniformName(id, i);
            var uni = uniforms.get(name);
            if (uni == null) {
                throw new IllegalArgumentException("No uniform with name: '" + name + "' present in meta.json");
            }
            uni.position = i;
        }
    }

    public byte id() { return id; }

    public VertexFormat vertexFormat() { return vertexFormat; }

    private static byte genId() {
        int id = glCreateProgram();
        if (id >= MAX_ID) {
            throw new IllegalStateException("Max shader id exceeded");
        }
        return (byte)id;
    }

    public static Shader load(String name,
                              String vertexSource, String fragmentSource,
                              VertexFormat vertexFormat, Map<String, Uniform> uniforms) {
        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        byte program = genId();
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

        var shader = new Shader(program, name, vertexFormat, uniforms);
        ResourceCache.shadersById[program] = shader;

        return shader;
    }

    private static int compileShader(int type, String source) {
        int glHandle = glCreateShader(type);
        if (glHandle == 0) {
            return 0; // error
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
        glUseProgram(id);
    }

    public void setUniformTexture2d(String name, Drawable tex) {
        setUniformTexture2d(name, tex, 0);
    }

    public void setUniformTexture2d(String name, Drawable tex, int unit) {
        setUniformTexture2d(name, tex.id(), unit);
    }

    public void setUniformTexture2d(String name, short texId, int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, texId);

        setUniformInt(name, unit);
    }

    public void setUniformFloat(String name, float val) {
        glUniform1f(uniformLocation(name), val);
    }

    public void setUniformInt(String name, int val) {
        glUniform1i(uniformLocation(name), val);
    }

    public void setUniformVec2f(String name, float x, float y) {
        glUniform2f(uniformLocation(name), x, y);
    }

    private static final float[] tmpMat3 = new float[9];

    public void setUniformMat3(String name, float[] val) {
        glUniformMatrix3fv(uniformLocation(name), false, val);
    }

    public void setUniformMat3(String name, Mat3 val) {
        setUniformMat3(name, val.val);
    }

    public void setUniformMat3(String name,
                               float m00, float m01, float m02,
                               float m10, float m11, float m12,
                               float m20, float m21, float m22) {
        float[] res = tmpMat3;

        res[M00] = m00; res[M01] = m01; res[M02] = m02;
        res[M10] = m10; res[M11] = m11; res[M12] = m12;
        res[M20] = m20; res[M21] = m21; res[M22] = m22;

        glUniformMatrix3fv(uniformLocation(name), false, res);
    }

    public int uniformLocation(String name) {
        Uniform uniform = uniforms.get(name);
        if (uniform != null) {
            return uniform.position;
        }
        throw new IllegalStateException("Invalid uniform name: '" + name + "' in " + this);
    }

    @Override
    public String toString() {
        return "Shader{" +
                "name='" + shaderName +
                "', id=" + id +
                ", vertexFormat=" + vertexFormat +
                ", uniforms=" + uniforms +
                '}';
    }

    @Override
    public void close() {
        ResourceCache.shadersById[id] = null;
        glDeleteProgram(id);
    }

    public static final class Uniform {

        private final Type type;

        private int position;

        public Uniform(Type type) {
            this.type = type;
        }

        public Type type()    { return type; }
        public int position() { return position; }

        @Override
        public String toString() {
            return "Uniform{" +
                    "type=" + type +
                    ", position=" + position +
                    '}';
        }

        public enum Type {
            TEXTURE2D,
            VEC2F,
            FLOAT,
            MATRIX3F
        }
    }
}
