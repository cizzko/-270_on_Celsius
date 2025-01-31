package core.input;

import core.EventHandling.Logging.Config;
import core.Global;
import core.World.Textures.TextureDrawing;
import core.math.Point2i;
import core.math.Vector2f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL46;

import java.util.ArrayList;
import java.util.Arrays;

import static core.EventHandling.Logging.Logger.printException;
import static core.Window.glfwWindow;
import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {
    static final int PRESSED_ARRAY_SIZE = 349;
    static final int CLICKED_ARRAY_SIZE = 8; // GLFW_MOUSE_BUTTON_1 ~ GLFW_MOUSE_BUTTON_8

    private final long[] pressed, clicked, repeated;
    private final long[] justPressed, justClicked;
    private final ArrayList<InputListener> listeners = new ArrayList<>();
    private final Point2i mousePos = new Point2i();
    private final Point2i mouseBlockPos = new Point2i();
    private final Vector2f mouseWorldPos = new Vector2f();

    private long lastMouseMoveTimestamp;
    private float scrollOffset = 1;
    private int width, height;

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
                lastMouseMoveTimestamp = System.currentTimeMillis();
                mousePos.set((int) xpos, (int) (height - ypos));
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
                    }
                    case GLFW_RELEASE -> {
                        unsetBit(pressed, key);
                        unsetBit(repeated, key);
                        setBit(justPressed, key);
                    }
                    case GLFW_REPEAT -> setBit(repeated, key);
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
                    }
                    case GLFW_RELEASE -> {
                        unsetBit(clicked, button);
                        setBit(justClicked, button);
                    }
                }
            }
        }));
        glfwSetScrollCallback(glfwWindow, Global.app.keep(new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                scrollOffset = Math.clamp((float)yoffset + scrollOffset, 0, 50);
            }
        }));
        glfwSetWindowSizeCallback(glfwWindow, Global.app.keep(new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int w, int h) {
                width = w;
                height = h;
                GL46.glViewport(0, 0, w, h);
                onResize(w, h);
            }
        }));
    }

    public void update() {
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

    private void onResize(int w, int h) {
        listeners.forEach(i -> i.onResize(w, h));
    }

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

    public long getLastMouseMoveTimestamp() {
        return lastMouseMoveTimestamp;
    }

    public Point2i mouseBlockPos() {
        var world = mouseWorldPos();
        mouseBlockPos.set((int) (world.x / TextureDrawing.blockSize), (int) (world.y / TextureDrawing.blockSize));
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

    private static long[] createBitSet(int n) {
        return new long[((n - 1) >> 6) + 1];
    }

    private static void setBit(long[] bits, int i) {
        int idx = i >> 6;
        if (idx < 0 || idx >= bits.length) {
            printException("Unexpected button: " + i, new IllegalArgumentException());
            return;
        }
        bits[idx] |= 1L << i;
    }

    private static void unsetBit(long[] bits, int i) {
        int idx = i >> 6;
        if (idx < 0 || idx >= bits.length) {
            printException("Unexpected button: " + i, new IllegalArgumentException());
            return;
        }
        bits[idx] &= ~(1L << i);
    }

    private static boolean isSet(long[] bits, int i) {
        int idx = i >> 6;
        if (idx < 0 || idx >= bits.length) {
            printException("Unexpected button: " + i, new IllegalArgumentException());
            return false;
        }
        return (bits[idx] & (1L << i)) != 0;
    }
}
