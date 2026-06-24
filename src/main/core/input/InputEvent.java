package core.input;

import java.lang.foreign.StructLayout;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.MemoryLayout.unionLayout;
import static java.lang.foreign.ValueLayout.*;

public final class InputEvent {
    private InputEvent() {}

    public static final int TYPE_MOUSE_RELEASE    = 0;
    public static final int TYPE_MOUSE_PRESS      = 1;
    public static final int TYPE_MOUSE_MOVE       = 2;
    public static final int TYPE_MOUSE_DRAG       = 3;

    public static final int TYPE_KEYBOARD_RELEASE = 4;
    public static final int TYPE_KEYBOARD_PRESS   = 5;
    public static final int TYPE_KEYBOARD_REPEAT  = 6;

    public static final int TYPE_SCROLL           = 7;
    public static final int TYPE_CODEPOINT        = 8;
    public static final int TYPE_FRAMEBUFFER      = 9;

    public static boolean isMouseMove(int type) {
        return type >= TYPE_MOUSE_MOVE && type <= TYPE_MOUSE_DRAG;
    }

    public static final StructLayout LAYOUT;
    public static final long BYTE_SIZE;

    static {
        LAYOUT = structLayout(
                JAVA_SHORT.withName("type"),
                JAVA_SHORT.withName("key"),
                unionLayout(
                        structLayout(
                                JAVA_FLOAT.withName("x"),
                                JAVA_FLOAT.withName("y")
                        ).withName("mouse"),
                        structLayout(
                                JAVA_INT.withName("scancode"),
                                JAVA_INT.withName("mods")
                        ).withName("keyboard"),
                        structLayout(
                                JAVA_FLOAT.withName("x"),
                                JAVA_FLOAT.withName("y")
                        ).withName("scroll"),
                        structLayout(
                                JAVA_INT.withName("codepoint")
                        ).withName("codepoint"),
                        structLayout(
                                JAVA_INT.withName("x"),
                                JAVA_INT.withName("y")
                        ).withName("framebuffer")
                ).withName("data")
        );

        BYTE_SIZE = LAYOUT.byteSize();
    }
}
