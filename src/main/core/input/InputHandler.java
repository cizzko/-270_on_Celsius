package core.input;

import core.GameState;
import core.Global;
import core.math.Point2i;
import core.math.Vector2d;
import core.math.Vector2f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.*;

import java.util.Arrays;

import static core.Window.*;
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

    private long lastMouseMoveTimestamp;
    private float scrollOffset = 1, scrollDelta = 0;
    private int width, height;
    private boolean anyMouseClick;

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

    public void init() {

        glfwSetCursorPosCallback(glfwWindow, Global.app.keep(new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                float y = (float) (height - ypos);
                float x = (float) xpos;
                lastMouseMoveTimestamp = System.currentTimeMillis();
                mousePos.set(x, y);
                updateMouseWorld();

                if (anyMouseClick) {
                    onMouseDragged(x, y);
                } else {
                    onMouseMove(x, y);
                }
            }
        }));
        glfwSetKeyCallback(glfwWindow, Global.app.keep(new GLFWKeyCallback() {
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
        glfwSetMouseButtonCallback(glfwWindow, Global.app.keep(new GLFWMouseButtonCallback() {
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
                        // setBit(justClicked, button);
                        setBit(releasedButtons, button);

                        anyMouseClick = false;
                        onTouchUp(mousePos.x, mousePos.y, button);
                    }
                }
            }
        }));
        glfwSetScrollCallback(glfwWindow, Global.app.keep(new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                float xoffsetf = (float) xoffset;
                float yoffsetf = (float) yoffset;
                scrollOffset = Math.clamp(yoffsetf + scrollOffset, 0, 50);
                scrollDelta = yoffsetf;
                onScroll(xoffsetf, yoffsetf);
            }
        }));
        glfwSetFramebufferSizeCallback(glfwWindow, Global.app.keep(new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int w, int h) {

                float aspect = (float) w / h;
                float defaultAspect = (float) defaultWidth/defaultHeight;
                if (Global.gameState == GameState.MENU) // TODO решить как поступать
                {
                    if (aspect >= defaultAspect) {
                        int viewW = (int)(h * defaultAspect);
                        glViewport((w - viewW)/2, 0, viewW, h);
                        w = viewW;
                    } else {
                        int viewH = (int)(w / defaultAspect);
                        glViewport(0, (h - viewH)/2, w, viewH);
                        h = viewH;
                    }
                } else {
                    glViewport(0, 0, w, h);
                }

                width = w;
                height = h;

                if (Global.gameState == GameState.PLAYING)
                    Global.camera.resizeViewport(w, h);
                onFramebufferResize(w, h);
            }
        }));
        glfwSetCharCallback(glfwWindow, Global.app.keep(new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                onCodepoint(codepoint);
            }
        }));
    }

    private void updateMouseWorld() {
        Global.camera.unprojectTo(mousePos, mouseWorldPos);
        mouseBlockPos.set(toBlock(mouseWorldPos.x), toBlock(mouseWorldPos.y));
    }

    public void update() {
        scrollDelta = 0;
        Arrays.fill(justPressed, 0);
        Arrays.fill(justClicked, 0);
        Arrays.fill(releasedButtons, 0);
        Arrays.fill(releasedKeys, 0);

        glfwPollEvents();
        updateMouseWorld();
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

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }

    public float getScrollOffset() {
        return scrollOffset;
    }

    public float scrollDelta() {
        return scrollDelta;
    }

    public long getLastMouseMoveTimestamp() {
        return lastMouseMoveTimestamp;
    }

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

    //для мыши
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
}
