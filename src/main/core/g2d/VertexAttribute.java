package core.g2d;

import org.lwjgl.opengl.GL46;

public final class VertexAttribute {
    public final int size;
    public final Type type;

    private final Interp interp;

    private VertexAttribute(int size, Type type, Interp interp) {
        this.size = size;
        this.type = type;
        this.interp = interp;
    }

    public static VertexAttribute create(int size, Type type, Interp interp) {
        return new VertexAttribute(size, type, interp);
    }

    public int byteSize() {
        return size * type.byteSize;
    }

    public void enable(int index, int vertexByteSize, int offset) {
        GL46.glEnableVertexAttribArray(index);
        interp.enable(index, size, type.glType, vertexByteSize, offset);
    }

    public void disable(int index) {
        GL46.glDisableVertexAttribArray(index);
    }

    public enum Interp {
        NORMAL {
            @Override
            void enable(int index, int size, int glType, int vertexByteSize, int offset) {
                GL46.glVertexAttribPointer(index, size, glType, false, vertexByteSize, offset);
            }
        },
        COLOR {
            @Override
            void enable(int index, int size, int glType, int vertexByteSize, int offset) {
                GL46.glVertexAttribPointer(index, size, glType, true, vertexByteSize, offset);
            }
        };

        abstract void enable(int index, int size, int glType, int vertexByteSize, int offset);
    }

    public enum Type {
        FLOAT(Float.BYTES, GL46.GL_FLOAT),
        UNSIGNED_BYTE(Byte.BYTES, GL46.GL_UNSIGNED_BYTE),
        BYTE(Byte.BYTES, GL46.GL_BYTE),
        UNSIGNED_SHORT(Short.BYTES, GL46.GL_UNSIGNED_SHORT),
        SHORT(Short.BYTES, GL46.GL_SHORT),
        UNSIGNED_INT(Integer.BYTES, GL46.GL_UNSIGNED_INT),
        INT(Integer.BYTES, GL46.GL_INT);

        public final int byteSize;
        public final int glType;

        Type(int byteSize, int glType) {
            this.byteSize = byteSize;
            this.glType = glType;
        }
    }
}
