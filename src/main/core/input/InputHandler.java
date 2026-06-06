package core.input;

import core.Global;
import core.WorldCoordinates;
import core.math.Point2i;
import core.math.Vector2f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.*;

import java.util.Arrays;

import static core.Window.glfwWindow;
import static core.util.FixedBitset.createBitSet;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class InputHandler {
    private static final Logger log = LogManager.getLogger();

    static final int PRESSED_ARRAY_SIZE = 349;
    static final int CLICKED_ARRAY_SIZE = 8; // GLFW_MOUSE_BUTTON_1 ~ GLFW_MOUSE_BUTTON_8

    private final long[] pressed, clicked, repeated;
    private final long[] justPressed, justClicked;
    private final ObjectArrayList<InputListener> listeners = new ObjectArrayList<>();
    private final Point2i mousePos = new Point2i();
    private final Point2i mouseBlockPos = new Point2i();
    private final Vector2f mouseWorldPos = new Vector2f();

    private long lastMouseMoveTimestamp;
    private float scrollOffset = 1, scrollChange = 0;
    private int width, height;
    private boolean anyMouseClick;

    public InputHandler(int width, int height) {
        this.width = width;
        this.height = height;

        justPressed = createBitSet(PRESSED_ARRAY_SIZE);
        justClicked = createBitSet(CLICKED_ARRAY_SIZE);

        repeated = createBitSet(PRESSED_ARRAY_SIZE);
        pressed = createBitSet(PRESSED_ARRAY_SIZE);
        clicked = createBitSet(CLICKED_ARRAY_SIZE);
    }

    public void init() {

        glfwSetCursorPosCallback(glfwWindow, Global.app.keep(new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                int y = (int) (height - ypos);
                int x = (int) xpos;
                lastMouseMoveTimestamp = System.currentTimeMillis();
                mousePos.set((int) xpos, (int) (height - ypos));
                mousePos.set(x, y);
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
                        setBit(justPressed, key);

                        onKeyDown(key, scancode);
                    }
                    case GLFW_RELEASE -> {
                        unsetBit(pressed, key);
                        unsetBit(repeated, key);
                        setBit(justPressed, key);

                        onKeyUp(key, scancode);
                    }
                    case GLFW_REPEAT -> {
                        setBit(repeated, key);

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

                        anyMouseClick = true;
                        onTouchDown(mousePos.x, mousePos.y, button);
                    }
                    case GLFW_RELEASE -> {
                        unsetBit(clicked, button);
                        setBit(justClicked, button);

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
                scrollChange = yoffsetf;
                onScroll(xoffsetf, yoffsetf);
            }
        }));
        glfwSetWindowSizeCallback(glfwWindow, Global.app.keep(new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int w, int h) {
                width = w;
                height = h;
                glViewport(0, 0, w, h);
                onResize(w, h);
            }
        }));
        glfwSetCharCallback(glfwWindow, Global.app.keep(new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                onCodepoint(codepoint);
            }
        }));
    }

    public void update() {
        scrollChange = 0;
        Arrays.fill(justPressed, 0);
        Arrays.fill(justClicked, 0);

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

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }

    public float getScrollOffset() {
        return scrollOffset;
    }

    public float getScrollChange() {
        return scrollChange;
    }

    public long getLastMouseMoveTimestamp() {
        return lastMouseMoveTimestamp;
    }

    public Point2i mouseBlockPos() {
        var world = mouseWorldPos();
        mouseBlockPos.set(WorldCoordinates.toBlock(world.x), WorldCoordinates.toBlock(world.y));
        return mouseBlockPos;
    }

    // Позиция в мире
    public Vector2f mouseWorldPos() {
        // Поскольку мы в праве менять проекция камеры, то и значение worldPos() всегда должно быть актуальным
        mouseWorldPos.set(mousePos.x, mousePos.y);
        return Global.camera.unproject(mouseWorldPos);
    }

    // Позиция на экране
    public Point2i mousePos() {
        return mousePos;
    }

    public boolean pressed(int keycode) {
        return isSet(pressed, keycode);
    }

    public boolean repeated(int keycode) {
        return isSet(repeated, keycode);
    }

    public boolean justPressed(int keycode) {
        return isSet(pressed, keycode) && isSet(justPressed, keycode);
    }

    public boolean clicked(int button) {
        return isSet(clicked, button);
    }

    //для мыши
    public boolean justClicked(int button) {
        return isSet(clicked, button) && isSet(justClicked, button);
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

    private void onResize(int w, int h) {
        listeners.forEach(i -> i.onResize(w, h));
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

    private void onTouchDown(int x, int y, int button) {
        listeners.forEach(listener -> listener.onTouchDown(x, y, button));
    }

    private void onTouchUp(int x, int y, int button) {
        listeners.forEach(listener -> listener.onTouchUp(x, y, button));
    }

    private void onScroll(float xOffset, float yOffset) {
        listeners.forEach(listener -> listener.onScroll(xOffset, yOffset));
    }

    private void onMouseMove(int x, int y) {
        listeners.forEach(listener -> listener.onMouseMove(x, y));
    }

    private void onMouseDragged(int x, int y) {
        listeners.forEach(listener -> listener.onMouseDragged(x, y));
    }

    private void onKeyUp(int key, int scancode) {
        listeners.forEach(listener -> listener.onKeyUp(key, scancode));
    }

    private void onKeyDown(int key, int scancode) {
        listeners.forEach(listener -> listener.onKeyDown(key, scancode));
    }
}
