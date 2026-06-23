package core.input;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static core.input.InputEvent.*;

public final class InputRingBuffer {

    private static final Logger log = LogManager.getLogger(InputRingBuffer.class);
    private final int[] buffer;
    private final int capacity;
    private final int mask;

    private int tail = 0;
    private int head = 0;

    public InputRingBuffer(int maxEvents) {
        if ((maxEvents & (maxEvents - 1)) != 0) {
            throw new IllegalArgumentException("Размер буфера должен быть степенью двойки!");
        }
        this.capacity = maxEvents;
        this.mask = maxEvents - 1;
        this.buffer = new int[capacity * (int) BYTE_SIZE];
    }

    public boolean writeScroll(float x, float y) {
        int currentTail = tail;
        int currentHead = head;

        if (currentTail - currentHead >= capacity) {
            discardWarn("scroll");
            return false;
        }

        int index = (currentTail & mask) * (int) BYTE_SIZE;

        buffer[index]     = (TYPE_SCROLL << 16);
        buffer[index + 1] = Float.floatToRawIntBits(x);
        buffer[index + 2] = Float.floatToRawIntBits(y);

        tail = currentTail + 1;
        return true;
    }

    public boolean writeFramebuffer(int w, int h) {
        int currentTail = tail;
        int currentHead = head;

        if (currentTail - currentHead >= capacity) {
            discardWarn("framebuffer");
            return false;
        }

        int index = (currentTail & mask) * (int) BYTE_SIZE;

        buffer[index]     = (TYPE_FRAMEBUFFER << 16);
        buffer[index + 1] = w;
        buffer[index + 2] = h;

        tail = currentTail + 1;
        return true;
    }

    public boolean writeKeyboardEvent(int type, int button, int scancode, int mods) {
        int currentTail = tail;
        int currentHead = head;

        if (currentTail - currentHead >= capacity) {
            discardWarn("keyboard");
            return false;
        }

        int index = (currentTail & mask) * (int) BYTE_SIZE;

        buffer[index]     = (type << 16) | (button & 0xFFFF);
        buffer[index + 1] = scancode;
        buffer[index + 2] = mods;

        tail = currentTail + 1;
        return true;
    }

    public boolean writeCodepoint(int codepoint, int mods) {
        int currentTail = tail;
        int currentHead = head;

        if (currentTail - currentHead >= capacity) {
            discardWarn("codepoint");
            return false;
        }

        int index = (currentTail & mask) * (int) BYTE_SIZE;

        buffer[index] = (TYPE_CODEPOINT << 16);
        buffer[index + 1] = codepoint;
        buffer[index + 2] = mods;

        tail = currentTail + 1;
        return true;
    }

    public boolean writeMouseEvent(int type, int button, float x, float y) {
        int currentTail = tail;
        int currentHead = head;

        if (isMouseMove(type) && currentTail > currentHead) {
            int prevTail = currentTail - 1;
            int prevIndex = (prevTail & mask) * (int) BYTE_SIZE;
            int prevHeader = buffer[prevIndex];

            if ((prevHeader >>> 16) == type) {
                buffer[prevIndex + 1] = Float.floatToRawIntBits(x);
                buffer[prevIndex + 2] = Float.floatToRawIntBits(y);
                return true;
            }
        }

        if (currentTail - currentHead >= capacity) {
            discardWarn("mouse");
            return false;
        }

        int index = (currentTail & mask) * (int) BYTE_SIZE;

        buffer[index]     = (type << 16) | (button & 0xFFFF);
        buffer[index + 1] = Float.floatToRawIntBits(x);
        buffer[index + 2] = Float.floatToRawIntBits(y);

        tail = currentTail + 1;
        return true;
    }

    public void readEvents(InputHandler processor) {
        int currentHead = head;
        int currentTail = tail;

        while (currentHead < currentTail) {
            int index = (currentHead & mask) * (int) BYTE_SIZE;

            int header = buffer[index];

            int type = header >>> 16;
            int code = header & 0xFFFF;

            switch (type) {
                case TYPE_MOUSE_PRESS, TYPE_MOUSE_RELEASE, TYPE_MOUSE_MOVE, TYPE_MOUSE_DRAG -> {
                    float x = Float.intBitsToFloat(buffer[index + 1]);
                    float y = Float.intBitsToFloat(buffer[index + 2]);
                    processor.processMouse(type, code, x, y);
                }
                case TYPE_KEYBOARD_PRESS, TYPE_KEYBOARD_REPEAT, TYPE_KEYBOARD_RELEASE -> {
                    int scancode = buffer[index + 1];
                    int mods     = buffer[index + 2];
                    processor.processKeyboard(type, code, scancode, mods);
                }
                case TYPE_SCROLL      -> {
                    float x = Float.intBitsToFloat(buffer[index + 1]);
                    float y = Float.intBitsToFloat(buffer[index + 2]);
                    processor.processScroll(x, y);
                }
                case TYPE_CODEPOINT   -> processor.processCodepoint(buffer[index + 1], buffer[index + 2]);
                case TYPE_FRAMEBUFFER -> processor.processFramebuffer(buffer[index + 1], buffer[index + 2]);
            }

            currentHead++;
        }

        head = currentHead;
    }

    private static void discardWarn(String name) {
        log.warn("{} event discarded: too many input events", name);
    }
}
