package core.g2d;

import core.math.Mat3;
import core.util.Disposable;

import java.util.Map;

import static org.lwjgl.opengl.GL46.*;

public final class Shader implements Disposable {
    public static final int MAX_ID = 1 << 8;

    final byte glHandle;

    final VertexFormat vertexFormat;
    final Map<String, Uniform> uniforms;

    private final float[] mat4adapt = new float[16];

    Shader(byte glHandle, VertexFormat vertexFormat, Map<String, Uniform> uniforms) {
        this.glHandle = glHandle;
        this.vertexFormat = vertexFormat;
        this.uniforms = Map.copyOf(uniforms);

        int uniformCount = glGetProgrami(glHandle, GL_ACTIVE_UNIFORMS);
        for (int i = 0; i < uniformCount; i++) {
            String name = glGetActiveUniformName(glHandle, i);
            var uni = uniforms.get(name);
            if (uni == null) {
                throw new IllegalArgumentException("No uniform with name: '" + name + "' present in meta.json");
            }
            uni.position = i;
        }
    }

    public byte id() { return glHandle; }

    public VertexFormat vertexFormat() { return vertexFormat; }

    private static byte genId() {
        int id = glCreateProgram();
        if (id >= MAX_ID)
            throw new IllegalStateException("Max shader id exceeded");
        return (byte)id;
    }

    public static Shader load(String vertexSource, String fragmentSource,
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

        var shader = new Shader(program, vertexFormat, uniforms);
        ShaderCache.shadersById.put(program, shader);

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
        glUseProgram(glHandle);
    }

    public void setUniform(String name, Texture tex) {
        setUniform(name, tex, 0);
    }

    public void setUniform(String name, Texture tex, int unit) {
        setUniform(name, tex.glHandle, unit);
    }

    public void setUniform(String name, short texId, int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, texId);

        setUniform(name, unit);
    }

    public void setUniform(String name, int val) {
        glUniform1i(uniformLocation(name), val);
    }

    public void setUniformTransforming(String name, float[] val) {
        float[] mat4 = mat4adapt;
        toMat4(val, mat4);
        glUniformMatrix4fv(uniformLocation(name), false, mat4);
    }

    public void setUniformTransforming(String name, Mat3 val) {
        setUniformTransforming(name, val.val);
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
        return uniforms.get(name).position;
    }

    @Override
    public void close() {
        ShaderCache.shadersById.remove(glHandle);
        glDeleteProgram(glHandle);
    }

    public static final class Uniform {

        private final Type type;

        private int position;

        public Uniform(Type type) {
            this.type = type;
        }


        public Type type()    { return type; }
        public int position() { return position; }

        public enum Type {
            TEXTURE2D,
            MATRIX4F
        }
    }
}
