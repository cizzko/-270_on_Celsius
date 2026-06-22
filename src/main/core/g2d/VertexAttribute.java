package core.g2d;

import static org.lwjgl.opengl.GL46C.*;

public final class VertexAttribute {
    public final int size;
    public final Type type;

    private final Format format;

    VertexAttribute(int size, Type type, Format format) {
        this.size = size;
        this.type = type;
        this.format = format;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VertexAttribute that)) return false;
        return size == that.size && type == that.type && format == that.format;
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + h + size;
        h += (h << 5) + h + type.hashCode();
        h += (h << 5) + h + format.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "VertexAttribute{" +
               "size=" + size +
               ", type=" + type +
               ", format=" + format +
               '}';
    }

    public static VertexAttribute create(int size, Type type, Format format) {
        return new VertexAttribute(size, type, format);
    }

    public int byteSize() {
        return size * type.byteSize;
    }

    public void enable(int vao, int index, int vertexByteSize, int offset) {

        glEnableVertexArrayAttrib(vao, index);

        // 2. Настраиваем формат (вызов уходит в твой enum/класс format)
        format.enable(vao, index, size, type.glType, vertexByteSize, offset);
        // 3. Стягиваем атрибут и точку привязки буфера в один индекс (для простоты делаем их одинаковыми)
        glVertexArrayAttribBinding(vao, index, 0);
    }

    public void enable(int index, int vertexByteSize, int offset) {
        glEnableVertexAttribArray(index);
        format.enable(index, size, type.glType, vertexByteSize, offset);
    }

    public void disable(int index) {
        glDisableVertexAttribArray(index);
    }

    public enum Format {
        DIRECT_FLOAT {
            @Override
            void enable(int vao, int index, int size, int glType, int vertexByteSize, int offset) {
                glVertexArrayAttribFormat(vao, index, size, glType, false, offset);
            }

            @Override
            void enable(int index, int size, int glType, int vertexByteSize, int offset) {
                glVertexAttribPointer(index, size, glType, false, vertexByteSize, offset);
            }
        },
        INTEGRAL {
            @Override
            void enable(int vao, int index, int size, int glType, int vertexByteSize, int offset) {
                glVertexArrayAttribIFormat(vao, index, size, glType, offset);
            }

            @Override
            void enable(int index, int size, int glType, int vertexByteSize, int offset) {
                glVertexAttribIPointer(index, size, glType, vertexByteSize, offset);
            }
        },
        NORMALIZED {
            @Override
            void enable(int vao, int index, int size, int glType, int vertexByteSize, int offset) {
                glVertexArrayAttribFormat(vao, index, size, glType, true, offset);
            }

            @Override
            void enable(int index, int size, int glType, int vertexByteSize, int offset) {
                glVertexAttribPointer(index, size, glType, true, vertexByteSize, offset);
            }
        };

        abstract void enable(int index, int size, int glType, int vertexByteSize, int offset);

        abstract void enable(int vao, int index, int size, int glType, int vertexByteSize, int offset);
    }

    public enum Type {
        FLOAT(Float.BYTES, GL_FLOAT),
        UNSIGNED_BYTE(Byte.BYTES, GL_UNSIGNED_BYTE),
        BYTE(Byte.BYTES, GL_BYTE),
        UNSIGNED_SHORT(Short.BYTES, GL_UNSIGNED_SHORT),
        SHORT(Short.BYTES, GL_SHORT),
        UNSIGNED_INT(Integer.BYTES, GL_UNSIGNED_INT),
        INT(Integer.BYTES, GL_INT);

        public final int byteSize;
        public final int glType;

        Type(int byteSize, int glType) {
            this.byteSize = byteSize;
            this.glType = glType;
        }
    }
}
