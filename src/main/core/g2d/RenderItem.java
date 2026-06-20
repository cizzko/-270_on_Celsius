package core.g2d;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;

public final class RenderItem {
    private RenderItem() {}

    public static final StructLayout LAYOUT;

    public static final long BYTE_SIZE;

    public static final VarHandle VERTEX_OFFSET;
    public static final VarHandle VERTEX_COUNT;
    public static final VarHandle INDEX_OFFSET;
    public static final VarHandle INDEX_COUNT;

    static {
        /*
        unsigned long sortKey   <-- Записан в параллельном массиве, с.м. комментарии в RenderList
        int vertexOffset
        short vertexCount
        int indexOffset
        short indexCount
        */
        // На деле мы переупорядочиваем поля чтобы добиться выровненных доступов

        var layout = MemoryLayout.structLayout(
                ValueLayout.JAVA_INT.withName("vertexOffset"),
                ValueLayout.JAVA_INT.withName("indexOffset"),
                ValueLayout.JAVA_SHORT.withName("vertexCount"),
                ValueLayout.JAVA_SHORT.withName("indexCount")
        ).withByteAlignment(4);

        VERTEX_OFFSET = field(layout, "vertexOffset");
        VERTEX_COUNT = field(layout, "vertexCount");
        INDEX_OFFSET = field(layout, "indexOffset");
        INDEX_COUNT = field(layout, "indexCount");

        BYTE_SIZE = layout.byteSize();
        LAYOUT = layout;
    }

    static VarHandle field(StructLayout layout, String name) {
        var field = layout.arrayElementVarHandle(groupElement(name));
        return MethodHandles.insertCoordinates(field, 1, 0L).withInvokeExactBehavior();
    }
}
