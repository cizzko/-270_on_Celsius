package core.g2d;

import core.math.Mat3;
import core.util.Disposable;

import java.util.Map;
import java.util.Objects;

import static org.lwjgl.opengl.GL46.*;

public final class Shader implements Disposable {
    public static final int MAX_ID = 1 << 8;

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
        ResourceCache.shadersById.put(program, shader);

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

    private static final float[] mat4adapt = new float[16];

    public void setUniformTransforming(String name, float[] val) {
        float[] mat4 = mat4adapt;
        toMat4(val, mat4);
        glUniformMatrix4fv(uniformLocation(name), false, mat4);
    }

    public void setUniformTransforming(String name, Mat3 val) {
        setUniformTransforming(name, val.val);
    }

    public void setUniformTransforming(String name,
                                       float m00, float m01, float m02,
                                       float m10, float m11, float m12,
                                       float m20, float m21, float m22) {
        float[] res = mat4adapt;
        res[0]  = m00;
        res[4]  = m01;
        res[12] = m02;

        res[1]  = m10;
        res[5]  = m11;
        res[10] = m22;

        res[13] = m12;
        res[15] = 1;
        glUniformMatrix4fv(uniformLocation(name), false, res);
    }

    private static void toMat4(float[] val, float[] res) {

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
        Uniform uniform = uniforms.get(name);
        Objects.requireNonNull(uniform, () -> "Invalid uniform name: '" + name + "' in " + this);
        return uniform.position;
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
        ResourceCache.shadersById.remove(id);
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
            MATRIX4F
        }
    }
}
