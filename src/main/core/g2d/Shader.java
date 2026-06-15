package core.g2d;

import core.gen.UniformRelocations;
import core.math.MathUtil;
import core.util.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static core.g2d.OpenGL.CAN_USE_EXPLICIT_UNIFORM_LOCATIONS;
import static org.lwjgl.opengl.GL46.*;

public final class Shader implements Disposable {
    public static final int MAX_ID = 1 << 8;

    public static final String META_EXT = ".meta.json";
    public static final String VERT_EXT = ".vert";
    public static final String FRAG_EXT = ".frag";

    private static final Logger log = LogManager.getLogger("Shader");

    final byte id;
    final String shaderName;

    final VertexFormat vertexFormat;
    final Map<String, Uniform> uniforms;
    final int tapeSize;
    final short[] relocationTable;

    Shader(byte id, String shaderName, VertexFormat vertexFormat, Map<String, Uniform> uniforms) {
        this.id = id;
        this.shaderName = shaderName;
        this.vertexFormat = vertexFormat;
        this.uniforms = Map.copyOf(uniforms);

        byte uniformCount = MathUtil.toByteExact(glGetProgrami(id, GL_ACTIVE_UNIFORMS));

        for (byte i = 0; i < uniformCount; i++) {
            String name = glGetActiveUniformName(id, i);
            var uni = uniforms.get(name);
            if (uni == null) {
                throw new IllegalArgumentException("No uniform with name: '" + name + "' present in meta.json");
            }
            int location = glGetUniformLocation(id, name);
            uni.position = MathUtil.toShortExact(location);
        }

        this.relocationTable = CAN_USE_EXPLICIT_UNIFORM_LOCATIONS
                ? null
                : UniformRelocations.computeTable(this);


        int size = 0;
        for (var value : uniforms.values()) {
            size ++;
            switch (value.type()) {
                case TEXTURE2D, FLOAT, INT -> size++;
                case VEC2F -> size += 2;
                case MATRIX3F -> size += 9;
            }
        }
        tapeSize = size;
    }

    public byte id() { return id; }

    public VertexFormat vertexFormat() { return vertexFormat; }

    public Map<String, Uniform> uniforms() {
        return uniforms;
    }

    public String name() {
        return shaderName;
    }

    private static byte genId() {
        int id = glCreateProgram();
        if (id >= MAX_ID) {
            throw new IllegalStateException("Max shader id exceeded");
        }
        return (byte)id;
    }

    short relocate(short location) {
        if (CAN_USE_EXPLICIT_UNIFORM_LOCATIONS) {
            return location;
        }
        return relocationTable[location];
    }

    public static Shader load(String name,
                              core.g2d.GLSLPreprocessor.PipelineResult pipelineResult,
                              VertexFormat vertexFormat, Map<String, Uniform> uniforms) {
        if (log.isTraceEnabled()) {
            log.trace("['{}'] Vertex shader", name);
            System.out.println(pipelineResult.modifiedVertexSource());
            log.trace("['{}'] Fragment shader", name);
            System.out.println(pipelineResult.modifiedFragmentSource());
        }

        int vertexShader = compileShader(GL_VERTEX_SHADER, pipelineResult.modifiedVertexSource());
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, pipelineResult.modifiedFragmentSource());

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

        vertexFormat = Render.setupVAO(vertexFormat);
        var shader = new Shader(program, name, vertexFormat, uniforms);
        ResourceCache.shadersById[program] = shader;

        return shader;
    }

    private static int compileShader(int type, String source) {
        int id = glCreateShader(type);
        if (id == 0) {
            log.error("Failed to compile shader program:");
            System.err.println(source);
            System.exit(1);
            return 0; // error
        }
        glShaderSource(id, source);
        glCompileShader(id);

        int status = glGetShaderi(id, GL_COMPILE_STATUS);
        if (status != GL_TRUE) {
            String log = glGetShaderInfoLog(id);
            glDeleteShader(id);

            String typeStr = switch (type) {
                case GL_VERTEX_SHADER -> "vertex";
                case GL_FRAGMENT_SHADER -> "fragment";
                default -> "unnamed(0x" + Integer.toHexString(type) + ")";
            };
            throw new IllegalArgumentException("Failed to compile " + typeStr + " shader:\n" + log);
        }
        return id;
    }

    public void use() {
        glUseProgram(id);
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
        Render.dispose(vertexFormat);
        glDeleteProgram(id);
    }

    public static final class Uniform {

        private final Type type;

        private short position;

        public Uniform(Type type) {
            this.type = type;
        }

        public Type type()    { return type; }
        public short position() { return position; }

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
            INT,
            MATRIX3F
        }
    }
}
