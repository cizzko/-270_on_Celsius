package core.g2d;

import core.Application;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.*;

import java.lang.foreign.MemorySegment;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.ARBBindlessTexture.*;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL46.*;

final class OpenGL {
    private OpenGL() {}

    public static void saveHandle(short glHandle) {
        if (GL_ARB_bindless_texture) {
            long texHandle = glGetTextureHandleARB(glHandle);
            BindlessBinding.handlesByTex[glHandle] = glHandle;
            glMakeTextureHandleResidentARB(texHandle);
        }
    }

    public static void deleteHandle(short glHandle) {
        if (GL_ARB_bindless_texture) {
            long texHandle = BindlessBinding.handlesByTex[glHandle];
            if (glIsTextureHandleResidentARB(texHandle)) {
                glMakeTextureHandleNonResidentARB(texHandle);
            }
        }
    }

    public static int createVertexArrays() {
        if (DSA)
            return glCreateVertexArrays();
        return glGenVertexArrays();
    }

    public static int createBuffer() {
        if (DSA)
            return glCreateBuffers();
        return glGenBuffers();
    }

    public static void setBufferData(int array, int target, MemorySegment segment, int usage) {
        long byteSize = segment.byteSize();
        long nativeAddress = segment.address();
        if (DSA) {
            GL46.nglNamedBufferData(array, byteSize, nativeAddress, usage);
        } else {
            glBindBuffer(target, array);
            nglBufferData(target, byteSize, nativeAddress, usage);
            glBindBuffer(target, 0);
        }
    }

    public static void setBufferData(int array, int target, IntBuffer buffer, int usage) {
        if (DSA) {
            GL46.glNamedBufferData(array, buffer, usage);
        } else {
            glBindBuffer(target, array);
            glBufferData(target, buffer, usage);
            glBindBuffer(target, 0);
        }
    }

    public static void bufferSubData(int vbo, int target, long byteOffset, long byteSize, MemorySegment segment) {
        long nativeAddress = segment.address() + byteOffset;
        if (DSA) {
            nglNamedBufferSubData(vbo, byteOffset, byteSize, nativeAddress);
        } else {
            nglBufferSubData(target, byteOffset, byteSize, nativeAddress);
        }
    }

    public static int createTextures(int target) {
        if (DSA) return glCreateTextures(target);
        return glGenTextures();
    }

    public static void bindTexture(int target, int id) {
        if (DSA)
            return;
        glBindTexture(target, id);
    }

    public static void textureParameteri(int target, int id, int pname, int param) {
        if (DSA)
            GL46.glTextureParameteri(id, pname, param);
        else
            glTexParameteri(target, pname, param);
    }

    public static void texStorage2D(int target, int id, int levels, int internalformat, int width, int height) {
        if (DSA) {
            GL46.glTextureStorage2D(id, levels, internalformat, width, height);
        } else {
            GL42.glTexStorage2D(target, levels, internalformat, width, height);
        }
    }

    public static void texSubImage2D(int target, int id, int level, int xoffset, int yoffset,
                                     int width, int height, int format, int type, MemorySegment pixels) {
        long nativeAddress = pixels.address();
        if (DSA) {
            GL46.nglTextureSubImage2D(id, level, xoffset, yoffset, width, height, format, type, nativeAddress);
        } else {
            GL46.nglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, nativeAddress);
        }
    }

    public static void bindBuffer(int target, int id) {
        if (OpenGL.DSA) {
            return;
        }
        glBindBuffer(target, id);
    }

    public static void UniformMatrix3fv(short shaderId, short loc, boolean transpose, float[] res) {
        if (DSA) {
            glProgramUniformMatrix3fv(shaderId, loc, transpose, res);
        } else {
            glUniformMatrix3fv(loc, transpose, res);
        }
    }

    public static void Uniform2f(short shaderId, short loc, float x, float y) {
        if (DSA) {
            glProgramUniform2f(shaderId, loc, x, y);
        } else {
            glUniform2f(loc, x, y);
        }
    }

    public static void Uniform1f(short shaderId, short loc, float v) {
        if (DSA) {
            glProgramUniform1f(shaderId, loc, v);
        } else {
            glUniform1f(loc, v);
        }
    }

    public static void Uniform1i(short shaderId, short loc, int i) {
        if (DSA) {
            glProgramUniform1i(shaderId, loc, i);
        } else {
            glUniform1i(loc, i);
        }
    }

    private static final class BindlessBinding {
        private static final long[] handlesByTex = new long[Texture.MAX_ID];
    }

    static final int MAJOR_VERSION;
    static final int MINOR_VERSION;
    static final int VER;
    static final boolean IS_CORE_PROFILE;
    static final boolean CAN_USE_EXPLICIT_UNIFORM_LOCATIONS, CAN_USE_EXPLICIT_OUT_LOCATIONS;

    static final boolean GL_ARB_bindless_texture;
    static final boolean DSA;

    static {
        int[] iptr = new int[1];
        glGetIntegerv(GL_MAJOR_VERSION, iptr);
        MAJOR_VERSION = iptr[0];
        glGetIntegerv(GL_MINOR_VERSION, iptr);
        MINOR_VERSION = iptr[0];


        glGetIntegerv(GL_CONTEXT_PROFILE_MASK, iptr);
        IS_CORE_PROFILE = (iptr[0] & GL_CONTEXT_CORE_PROFILE_BIT) != 0;

        VER = ((MAJOR_VERSION * 100) + (MINOR_VERSION * 10));
        var caps = GL.getCapabilities();
        var GL_ARB_explicit_uniform_location = caps.GL_ARB_explicit_uniform_location;
        var GL_ARB_separate_shader_objects = caps.GL_ARB_separate_shader_objects;
        GL_ARB_bindless_texture = caps.GL_ARB_bindless_texture;
        // GL_ARB_bindless_texture = false;
        DSA = VER >= 450 || caps.GL_ARB_direct_state_access;
        // DSA = false;
        CAN_USE_EXPLICIT_UNIFORM_LOCATIONS =
                VER >= 420 || GL_ARB_explicit_uniform_location
        ;

        CAN_USE_EXPLICIT_OUT_LOCATIONS = VER >= 420 || GL_ARB_separate_shader_objects;

        Application.log.trace("[OpenGL] GL_ARB_bindless_texture: {}", GL_ARB_bindless_texture);
        Application.log.trace("[OpenGL] CAN_USE_EXPLICIT_UNIFORM_LOCATIONS: {}", CAN_USE_EXPLICIT_UNIFORM_LOCATIONS);
        Application.log.trace("[OpenGL] DSA: {}", DSA);
    }

    public static void bindTexture(short shaderId, short loc, short texId, int unit) {
        if (GL_ARB_bindless_texture) {
            if (DSA) glProgramUniformHandleui64ARB(shaderId, loc, BindlessBinding.handlesByTex[texId]);
            else     glUniformHandleui64ARB(loc, BindlessBinding.handlesByTex[texId]);
        } else {
            if (DSA) glBindTextureUnit(unit, texId);
            else {
                glActiveTexture(GL_TEXTURE0 + unit);
                GL11.glBindTexture(GL_TEXTURE_2D, texId);
            }
            Uniform1i(shaderId, loc, unit);
        }
    }

    public static List<String> computeInjectedText() {
        var baseList = new ObjectArrayList<String>(4);
        String versionStr = "#version " + VER + (IS_CORE_PROFILE ? " core" : "");
        baseList.add(versionStr);

        var caps = GL.getCapabilities();

        if (VER < 420 && caps.GL_ARB_explicit_uniform_location)
            baseList.add("#extension GL_ARB_explicit_uniform_location : require");
        if (VER < 420 && caps.GL_ARB_separate_shader_objects)
            baseList.add("#extension GL_ARB_separate_shader_objects : require");
        if (caps.GL_ARB_bindless_texture)
            baseList.add("#extension GL_ARB_bindless_texture : require");
        return baseList;
    }
}
