package core.input;

import core.Window;
import core.math.MathUtil;
import core.math.Point2i;
import core.math.Vector2d;
import core.math.Vector2f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryStack;

import java.util.Arrays;

import static core.Global.*;
import static core.Window.glfwHandle;
import static core.WorldCoordinates.toBlock;
import static core.input.InputEvent.*;
import static core.util.FixedBitset.createBitSet;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.glViewport;

public final class InputHandler {
    private static final Logger log = LogManager.getLogger();

    static final int PRESSED_ARRAY_SIZE = GLFW_KEY_LAST + 1;
    static final int CLICKED_ARRAY_SIZE = GLFW_MOUSE_BUTTON_LAST + 1;
    static {
        if (CLICKED_ARRAY_SIZE != 8)
            throw new ExceptionInInitializerError("GLFW version inconsistency");
    }

    private int justPressedMouse, pressedMouse, releasedMouse;
    private int pressedMouseCount;

    private final long[] justPressedKeyboard, pressedKeyboard, repeatedKeyboard, releasedKeyboard;
    private final ObjectArrayList<InputListener> listeners = new ObjectArrayList<>();
    private final Vector2f mousePos = new Vector2f();
    private final Point2i mouseBlockPos = new Point2i();
    private final Vector2d mouseWorldPos = new Vector2d();

    private final InputRingBuffer inputRingBuffer = new InputRingBuffer(512);

    private float scrollOffset = 1, scrollDelta = 0;

    private double pendingRawX, pendingRawY;
    private boolean framebufferMustBeResized;

    // окно
    private int width, height;
    // Вьюпорт. Первая инициализация происходит в коллбеке на нормальных системах
    private int vx, vy, vw, vh;

    public InputHandler(int width, int height) {
        this.width = width;
        this.height = height;

        justPressedKeyboard = createBitSet(PRESSED_ARRAY_SIZE);
        releasedKeyboard    = createBitSet(PRESSED_ARRAY_SIZE);
        repeatedKeyboard    = createBitSet(PRESSED_ARRAY_SIZE);
        pressedKeyboard     = createBitSet(PRESSED_ARRAY_SIZE);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void init() {
        glfwSetWindowSizeCallback(glfwHandle, app.keep(new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int newWidth, int newHeight) {
                width  = newWidth;
                height = newHeight;
            }
        }));
        glfwSetCursorPosCallback(glfwHandle, app.keep(new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double rawX, double rawY) {
                pendingRawX = rawX;
                pendingRawY = rawY;

                double localX = pendingRawX - vx;
                double localY = height - pendingRawY - vy;

                float mx = (float)Math.clamp(localX, 0, vw);
                float my = (float)Math.clamp(localY, 0, vh);

                int action = pressedMouseCount > 0
                        ? InputEvent.TYPE_MOUSE_DRAG
                        : InputEvent.TYPE_MOUSE_MOVE;

                inputRingBuffer.writeMouseEvent(action, -1, mx, my);
            }
        }));
        glfwSetKeyCallback(glfwHandle, app.keep(new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                // При запуске с xwayland я получаю несколько ивентов с таким вот интересным параметром
                if (key == GLFW_KEY_UNKNOWN) {
                    return;
                }

                int inputEventType = TYPE_KEYBOARD_RELEASE + action;
                inputRingBuffer.writeKeyboardEvent(inputEventType, key, scancode, mods);
            }
        }));
        glfwSetMouseButtonCallback(glfwHandle, app.keep(new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                double localX = pendingRawX - vx;
                double localY = height - pendingRawY - vy;

                float mx = (float)Math.clamp(localX, 0, vw);
                float my = (float)Math.clamp(localY, 0, vh);

                int inputEventType = TYPE_MOUSE_RELEASE + action;
                if (inputEventType == TYPE_MOUSE_PRESS) {
                    pressedMouseCount ++;
                } else {
                    pressedMouseCount = Math.max(0, pressedMouseCount - 1);
                }

                inputRingBuffer.writeMouseEvent(inputEventType, button, mx, my);
            }
        }));
        glfwSetScrollCallback(glfwHandle, app.keep(new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                float xoffsetf = (float) xoffset;
                float yoffsetf = (float) yoffset;
                inputRingBuffer.writeScroll(xoffsetf, yoffsetf);
            }
        }));
        glfwSetFramebufferSizeCallback(glfwHandle, app.keep(new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int w, int h) {
                updateViewport(w, h);
                framebufferMustBeResized = true;
                inputRingBuffer.writeFramebuffer(w, h);
            }
        }));
        glfwSetCharModsCallback(glfwHandle, app.keep(new GLFWCharModsCallback() {
            @Override
            public void invoke(long window, int codepoint, int mods) {
                inputRingBuffer.writeCodepoint(codepoint, mods);
            }
        }));
    }

    public void updateMouse(float x, float y) {
        var ms = mousePos;
        var mw = mouseWorldPos;
        ms.set(x, y);
        camera.unprojectTo(ms, mw);
        mouseBlockPos.set(toBlock(mw.x), toBlock(mw.y));
    }

    public void update() {
        scrollDelta = 0;
        Arrays.fill(justPressedKeyboard, 0);
        Arrays.fill(releasedKeyboard, 0);

        justPressedMouse = 0;
        releasedMouse = 0;

        glfwPollEvents();

        inputRingBuffer.readEvents(this);

        {
            double localX = pendingRawX - vx;
            double localY = height - pendingRawY - vy;

            float mx = (float)Math.clamp(localX, 0, vw);
            float my = (float)Math.clamp(localY, 0, vh);

            updateMouse(mx, my);
        }

        if (framebufferMustBeResized) {
            framebufferMustBeResized = false;

            renderThread.execute(() -> glViewport(vx, vy, vw, vh));
        }
    }

    public void addListener(InputListener listener) {
        if (listeners.contains(listener)) {
            return;
        }
        listeners.add(listener);
    }

    // region InputListener

    // endregion
    // region Public API

    public int windowWidth()    { return width; }
    public int windowHeight()   { return height; }
    public int viewportWidth()  { return vw; }
    public int viewportHeight() { return vh; }

    public float scrollOffset() { return scrollOffset; }
    public float scrollDelta()  { return scrollDelta; }

    // Не модифицируйте содержимое векторов возвращаемых методами
    // mouseBlockPos(), mouseWorldPos(), mousePos()
    // А если захотели - будьте добры, делайте это через updateMouse(x, y)

    public Point2i mouseBlockPos() { return mouseBlockPos; }

    // Позиция в мире
    public Vector2d mouseWorldPos() { return mouseWorldPos; }

    // Позиция на экране
    public Vector2f mousePos() { return mousePos; }

    public boolean pressed(int keycode)     { return isSet(pressedKeyboard, keycode); }
    public boolean repeated(int keycode)    { return isSet(repeatedKeyboard, keycode); }
    public boolean justPressed(int keycode) { return isSet(justPressedKeyboard, keycode); }
    public boolean releasedKey(int keycode) { return isSet(releasedKeyboard, keycode); }

    public boolean releasedButton(int button) { return (releasedMouse & (1 << button)) != 0; }
    public boolean clicked(int button)        { return (pressedMouse & (1 << button)) != 0; }
    public boolean justClicked(int button)    { return (justPressedMouse & (1 << button)) != 0; }

    // По сути этот метод должен возвращать значения float [-1, 1]
    // если у нас есть аналоговая штука по типу геймпада, но на дискретных инпутах
    // всё немного по-другому
    public int axis(int keycodeMin, int keycodeMax) {
        boolean isMin = pressed(keycodeMin);
        boolean isMax = pressed(keycodeMax);
        if (isMin && isMax) {
            return 0;
        } else if (isMin) {
            return -1;
        } else if (isMax) {
            return 1;
        } else {
            return 0;
        }
    }

    // endregion

    private void onViewport(int vx, int vy, int vw, int vh) {
        camera.resizeViewport(vw, vh);

        listeners.forEach(i -> i.onViewport(vx, vy, vw, vh));
    }

    private static void setBit(long[] bits, int i) {
        int idx = i >> 6;
        if (idx >= 0 && idx < bits.length) {
            bits[idx] |= 1L << i;
        } else {
            log.error("Unexpected button: {}", i, new Exception());
        }
    }

    private static void unsetBit(long[] bits, int i) {
        int idx = i >> 6;
        if (idx >= 0 && idx < bits.length) {
            bits[idx] &= ~(1L << i);
        } else {
            log.error("Unexpected button: {}", i, new Exception());
        }
    }

    private static boolean isSet(long[] bits, int i) {
        int idx = i >> 6;
        if (idx >= 0 && idx < bits.length) {
            return (bits[idx] & (1L << i)) != 0;
        }
        log.error("Unexpected button: {}", i, new Exception());
        return false;
    }

    public void setClipboardText(@Nullable CharSequence text) {
        glfwSetClipboardString(glfwHandle, text);
    }

    public @Nullable String getClipboardText() {
        return glfwGetClipboardString(glfwHandle);
    }

    public void updateSize() {
        try (var st = MemoryStack.stackPush()) {
            var pX = st.mallocInt(1);
            var pY = st.mallocInt(1);
            glfwGetWindowSize(glfwHandle, pX, pY);
            setSize(pX.get(), pY.get());
        }
    }

    private void updateViewport(int w, int h) {
        float aspect = (float) w / h;
        float targetAspect = Window.targetAspect;

        int vx, vy, vw, vh;
        if (MathUtil.equalsEps(aspect, targetAspect, 0.15f)) {
            vx = vy = 0;
            vw = w;
            vh = h;
        } else if (aspect >= targetAspect) {
            int viewW = (int)(h * targetAspect);
            vx = (w - viewW)/2;
            vy = 0;
            vw = viewW;
            vh = h;
        } else {
            int viewH = (int)(w / targetAspect);
            vx = 0;
            vy = (h - viewH)/2;
            vw = w;
            vh = viewH;
        }

        this.vx = vx;
        this.vy = vy;
        this.vw = vw;
        this.vh = vh;
    }

    public void setViewportSize(int w, int h) {
        updateViewport(w, h);
        renderThread.execute(() -> glViewport(vx, vy, vw, vh));
        onViewport(vx, vy, vw, vh);
    }

    public void processMouse(int type, int code, float x, float y) {
        switch (type) {
            case TYPE_MOUSE_RELEASE -> {
                int mask = 1 << code;
                pressedMouse  &= ~mask;
                releasedMouse |= mask;
                listeners.forEach(listener -> listener.onTouchUp(x, y, code));
            }
            case TYPE_MOUSE_PRESS   -> {
                int mask = 1 << code;
                pressedMouse     |= mask;
                justPressedMouse |= mask;
                releasedMouse    &= ~mask;
                listeners.forEach(listener -> listener.onTouchDown(x, y, code));
            }
            case TYPE_MOUSE_MOVE    -> listeners.forEach(listener -> listener.onMouseMove(x, y));
            case TYPE_MOUSE_DRAG    -> listeners.forEach(listener -> listener.onMouseDragged(x, y));
        }
    }

    public void processKeyboard(int type, int code, int scancode, int mods) {
        switch (type) {
            case TYPE_KEYBOARD_RELEASE -> {
                unsetBit(pressedKeyboard, code);
                unsetBit(repeatedKeyboard, code);
                setBit(releasedKeyboard, code);
                listeners.forEach(listener -> listener.onKeyUp(code, scancode, mods));
            }
            case TYPE_KEYBOARD_PRESS   -> {
                setBit(pressedKeyboard, code);
                unsetBit(releasedKeyboard, code);
                setBit(justPressedKeyboard, code);
                listeners.forEach(listener -> listener.onKeyDown(code, scancode, mods));
            }
            case TYPE_KEYBOARD_REPEAT  -> {
                setBit(repeatedKeyboard, code);
                unsetBit(releasedKeyboard, code);
                listeners.forEach(listener -> listener.onKeyRepeat(code, scancode, mods));
            }
        }
    }

    public void processCodepoint(int codepoint, int mods) {
        listeners.forEach(listener -> listener.onCodepoint(codepoint, mods));
    }

    public void processScroll(float x, float y) {
        scrollOffset = Math.clamp(y + scrollOffset, 0, 50);
        scrollDelta = y;
        listeners.forEach(listener -> listener.onScroll(x, y));
    }

    public void processFramebuffer(int width, int height) {
        updateViewport(width, height); // чисто математика. состояние gl обновляется только 1 раз
        onViewport(vx, vy, vw, vh);
    }

    public void onFocus(boolean focused) {
        if (!focused)
            pressedMouseCount = 0;
    }
}
