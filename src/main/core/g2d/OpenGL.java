package core.g2d;

import core.Application;
import core.Global;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.lang.foreign.MemorySegment;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.ARBBindlessTexture.*;
import static org.lwjgl.opengl.GL46C.*;

final class OpenGL {
    private OpenGL() {}

    public static void saveHandle(int glHandle) {
        if (GL_ARB_bindless_texture) {
            Global.renderThread.schedule(() -> {
                long texHandle = glGetTextureHandleARB(glHandle);
                BindlessBinding.handlesByTex[glHandle] = texHandle;
                glMakeTextureHandleResidentARB(texHandle);
            });
        }
    }

    public static void deleteHandle(int glHandle) {
        if (GL_ARB_bindless_texture) {
            long texHandle = BindlessBinding.handlesByTex[glHandle];
            Global.renderThread.schedule(() -> {
                if (glIsTextureHandleResidentARB(texHandle)) {
                    glMakeTextureHandleNonResidentARB(texHandle);
                }
            });
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
            nglNamedBufferData(array, byteSize, nativeAddress, usage);
        } else {
            glBindBuffer(target, array);
            nglBufferData(target, byteSize, nativeAddress, usage);
            glBindBuffer(target, 0);
        }
    }

    public static void setBufferData(int array, int target, IntBuffer buffer, int usage) {
        if (DSA) {
            glNamedBufferData(array, buffer, usage);
        } else {
            glBindBuffer(target, array);
            glBufferData(target, buffer, usage);
            glBindBuffer(target, 0);
        }
    }

    public static void bufferSubData(int vbo, int target, long byteOffset, long byteSize, MemorySegment segment) {
        MemorySegment slice = segment.asSlice(byteOffset, byteSize);
        long nativeAddress = slice.address();
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
            glTextureParameteri(id, pname, param);
        else
            glTexParameteri(target, pname, param);
    }

    public static void texStorage2D(int target, int id, int levels, int internalformat, int width, int height) {
        if (DSA) {
            glTextureStorage2D(id, levels, internalformat, width, height);
        } else {
            glTexStorage2D(target, levels, internalformat, width, height);
        }
    }

    public static void texSubImage2D(int target, int id, int level, int xoffset, int yoffset,
                                     int width, int height, int format, int type, MemorySegment pixels) {
        long nativeAddress = pixels.address();
        if (DSA) {
            nglTextureSubImage2D(id, level, xoffset, yoffset, width, height, format, type, nativeAddress);
        } else {
            nglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, nativeAddress);
        }
    }

    public static void bindBuffer(int target, int id) {
        if (OpenGL.DSA) {
            return;
        }
        glBindBuffer(target, id);
    }

    public static void UniformMatrix3fv(int shaderId, int loc, boolean transpose, float[] res) {
        if (DSA) {
            glProgramUniformMatrix3fv(shaderId, loc, transpose, res);
        } else {
            glUniformMatrix3fv(loc, transpose, res);
        }
    }

    public static void Uniform2f(int shaderId, int loc, float x, float y) {
        if (DSA) {
            glProgramUniform2f(shaderId, loc, x, y);
        } else {
            glUniform2f(loc, x, y);
        }
    }

    public static void Uniform1f(int shaderId, int loc, float v) {
        if (DSA) {
            glProgramUniform1f(shaderId, loc, v);
        } else {
            glUniform1f(loc, v);
        }
    }

    public static void Uniform1i(int shaderId, int loc, int i) {
        if (DSA) {
            glProgramUniform1i(shaderId, loc, i);
        } else {
            glUniform1i(loc, i);
        }
    }

    public static void bindVertexArray(int vao) {
        if (DSA)
            return;
        glBindVertexArray(vao);
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

        var render = Global.gameSettings.render;

        // Некоторые драйверы врут.
        // Версии декларируемые ими зачастую не соответствуют возможностям
        // или реализация содержит множество багов. Поэтому автоматическое включение
        // многих флагов приведет в среднем только пессимизации и ужасным глюкам
        //    Конечно, должен быть способ по железу отсеять совсем старьё...

        GL_ARB_bindless_texture            = render.bindlessTextures && caps.GL_ARB_bindless_texture;
        DSA                                = render.dsa && VER >= 450 || caps.GL_ARB_direct_state_access;

        CAN_USE_EXPLICIT_UNIFORM_LOCATIONS = VER >= 420 || GL_ARB_explicit_uniform_location;
        CAN_USE_EXPLICIT_OUT_LOCATIONS     = VER >= 420 || GL_ARB_separate_shader_objects;

        Application.log.trace("[OpenGL] GL_ARB_bindless_texture: {}", GL_ARB_bindless_texture);
        Application.log.trace("[OpenGL] CAN_USE_EXPLICIT_UNIFORM_LOCATIONS: {}", CAN_USE_EXPLICIT_UNIFORM_LOCATIONS);
        Application.log.trace("[OpenGL] DSA: {}", DSA);
    }

    public static void bindTexture(int shaderId, int loc, short texId, int unit) {
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
