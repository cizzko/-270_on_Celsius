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
import static core.util.FixedBitset.createBitSet;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public final class InputHandler {
    private static final Logger log = LogManager.getLogger();

    static final int PRESSED_ARRAY_SIZE = 349;
    static final int CLICKED_ARRAY_SIZE = 8; // GLFW_MOUSE_BUTTON_1 ~ GLFW_MOUSE_BUTTON_8

    private final long[] pressed, clicked, repeated;
    private final long[] releasedKeys, releasedButtons;
    private final long[] justPressed, justClicked;
    private final ObjectArrayList<InputListener> listeners = new ObjectArrayList<>();
    private final Vector2f mousePos = new Vector2f();
    private final Point2i mouseBlockPos = new Point2i();
    private final Vector2d mouseWorldPos = new Vector2d();

    private float scrollOffset = 1, scrollDelta = 0;
    private boolean anyMouseClick;

    // окно
    private int width, height;
    // Вьюпорт. Первая инициализация происходит в коллбеке
    private int vx, vy, vw, vh;

    public InputHandler(int width, int height) {
        this.width = width;
        this.height = height;

        justPressed = createBitSet(PRESSED_ARRAY_SIZE);
        justClicked = createBitSet(CLICKED_ARRAY_SIZE);

        releasedKeys = createBitSet(PRESSED_ARRAY_SIZE);
        releasedButtons = createBitSet(CLICKED_ARRAY_SIZE);

        repeated = createBitSet(PRESSED_ARRAY_SIZE);
        pressed = createBitSet(PRESSED_ARRAY_SIZE);
        clicked = createBitSet(CLICKED_ARRAY_SIZE);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void init() {
        glfwSetWindowSizeCallback(glfwHandle, app.keep(new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                InputHandler.this.width = width;
                InputHandler.this.height = height;
            }
        }));
        glfwSetCursorPosCallback(glfwHandle, app.keep(new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double rawX, double rawY) {
                double localX = rawX - vx;
                double localY = height - rawY - vy;

                float mx = (float)Math.clamp(localX, 0, vw);
                float my = (float)Math.clamp(localY, 0, vh);

                updateMouse(mx, my);

                if (anyMouseClick) {
                    onMouseDragged(mx, my);
                } else {
                    onMouseMove(mx, my);
                }
            }
        }));
        glfwSetKeyCallback(glfwHandle, app.keep(new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                // При запуске с xwayland я получаю несколько ивентов с таким вот интересным параметром
                if (key == GLFW_KEY_UNKNOWN) {
                    return;
                }
                switch (action) {
                    case GLFW_PRESS -> {
                        setBit(pressed, key);
                        unsetBit(releasedKeys, key);
                        setBit(justPressed, key);

                        onKeyDown(key, scancode);
                    }
                    case GLFW_RELEASE -> {
                        unsetBit(pressed, key);
                        unsetBit(repeated, key);
                        setBit(releasedKeys, key);

                        onKeyUp(key, scancode);
                    }
                    case GLFW_REPEAT -> { // Skat: по факту не вызывается у меня
                        setBit(repeated, key);
                        unsetBit(releasedKeys, key);

                        onKeyRepeat(key, scancode);
                    }
                }
            }
        }));
        glfwSetMouseButtonCallback(glfwHandle, app.keep(new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                switch (action) {
                    case GLFW_PRESS -> {
                        setBit(clicked, button);
                        setBit(justClicked, button);
                        unsetBit(releasedButtons, button);

                        anyMouseClick = true;
                        onTouchDown(mousePos.x, mousePos.y, button);
                    }
                    case GLFW_RELEASE -> {
                        unsetBit(clicked, button);
                        setBit(releasedButtons, button);

                        anyMouseClick = false;
                        onTouchUp(mousePos.x, mousePos.y, button);
                    }
                }
            }
        }));
        glfwSetScrollCallback(glfwHandle, app.keep(new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                float xoffsetf = (float) xoffset;
                float yoffsetf = (float) yoffset;
                scrollOffset = Math.clamp(yoffsetf + scrollOffset, 0, 50);
                scrollDelta = yoffsetf;
                onScroll(xoffsetf, yoffsetf);
            }
        }));
        glfwSetFramebufferSizeCallback(glfwHandle, app.keep(new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int w, int h) {
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

                InputHandler.this.vx = vx;
                InputHandler.this.vy = vy;
                InputHandler.this.vw = vw;
                InputHandler.this.vh = vh;

                glViewport(vx, vy, vw, vh);
                onViewport(vx, vy, vw, vh);

                camera.resizeViewport(vw, vh);
                onFramebufferResize(vw, vh);
            }
        }));
        glfwSetCharCallback(glfwHandle, app.keep(new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                onCodepoint(codepoint);
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
        Arrays.fill(justPressed, 0);
        Arrays.fill(justClicked, 0);
        Arrays.fill(releasedButtons, 0);
        Arrays.fill(releasedKeys, 0);

        glfwPollEvents();
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

    public float scrollOffset() {
        return scrollOffset;
    }

    public float scrollDelta() {
        return scrollDelta;
    }

    // Не модифицируйте содержимое векторов возвращаемых методами
    // mouseBlockPos(), mouseWorldPos(), mousePos()
    // А если захотели - будьте добры, делайте это через updateMouse(x, y)

    public Point2i mouseBlockPos() { return mouseBlockPos; }

    // Позиция в мире
    public Vector2d mouseWorldPos() { return mouseWorldPos; }

    // Позиция на экране
    public Vector2f mousePos() { return mousePos; }

    public boolean pressed(int keycode) {
        return isSet(pressed, keycode);
    }

    public boolean repeated(int keycode) {
        return isSet(repeated, keycode);
    }

    public boolean justPressed(int keycode) {
        return isSet(justPressed, keycode);
    }

    public boolean releasedKey(int keycode) { return isSet(releasedKeys, keycode); }

    public boolean releasedButton(int button) { return isSet(releasedButtons, button); }

    public boolean clicked(int button) {
        return isSet(clicked, button);
    }

    public boolean justClicked(int button) {
        return isSet(justClicked, button);
    }

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

    private void onViewport(int x, int y, int w, int h) {
        listeners.forEach(i -> i.onViewport(x, y, w, h));
    }

    private void onFramebufferResize(int w, int h) {
        listeners.forEach(i -> i.onFramebufferResize(w, h));
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


    private void onKeyRepeat(int key, int scancode) {
        listeners.forEach(listener -> listener.onKeyRepeat(key, scancode));
    }

    private void onCodepoint(int codepoint) {
        listeners.forEach(listener -> listener.onCodepoint(codepoint));
    }

    private void onTouchDown(float x, float y, int button) {
        listeners.forEach(listener -> listener.onTouchDown(x, y, button));
    }

    private void onTouchUp(float x, float y, int button) {
        listeners.forEach(listener -> listener.onTouchUp(x, y, button));
    }

    private void onScroll(float xOffset, float yOffset) {
        listeners.forEach(listener -> listener.onScroll(xOffset, yOffset));
    }

    private void onMouseMove(float x, float y) {
        listeners.forEach(listener -> listener.onMouseMove(x, y));
    }

    private void onMouseDragged(float x, float y) {
        listeners.forEach(listener -> listener.onMouseDragged(x, y));
    }

    private void onKeyUp(int key, int scancode) {
        listeners.forEach(listener -> listener.onKeyUp(key, scancode));
    }

    private void onKeyDown(int key, int scancode) {
        listeners.forEach(listener -> listener.onKeyDown(key, scancode));
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
}
