package core.EventHandling;

import core.EventHandling.Logging.Config;
import core.Global;
import core.UI;
import core.graphic.Layer;
import core.ui.Dialog;
import core.Utils.SimpleColor;
import core.World.Creatures.Physics;
import core.World.Creatures.Player.Player;
import core.math.Point2i;
import core.ui.Element;
import core.ui.TextArea;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.opengl.GL46;

import java.util.function.Supplier;

import static core.EventHandling.Logging.Logger.log;
import static core.Global.*;
import static core.Utils.Commandline.updateLine;
import static core.Utils.NativeResources.addResource;
import static core.Window.*;
import static core.World.Textures.TextureDrawing.drawText;
import static org.lwjgl.glfw.GLFW.*;

public class EventHandler {
    private static boolean keyLogging = false;
    public static final StringBuilder keyLoggingText = new StringBuilder(256);
    public static int width = defaultWidth, height = defaultHeight, debugLevel = Integer.parseInt(Config.getFromConfig("Debug"));

    private static final class DebugBox extends TextArea {
        private final Supplier<String> format;

        private DebugBox(Supplier<String> format) {
            this.format = format;
            this.color = SimpleColor.DIRTY_BRIGHT_BLACK;
        }

        @Override
        public void update() {
            text = format.get();
        }

        @Override
        public void draw() {
            if (!visible) {
                return;
            }
            if (text != null) {
                batch.draw(Layer.DEBUG, () -> drawText(x, y, text, color));
            }
        }
    }
    private static final Dialog debugDialog = new Dialog();

    public static void setKeyLoggingText(String text) {
        keyLoggingText.setLength(0);
        keyLoggingText.append(text);
    }

    private static void initCallbacks() {
        log("Thread: Event handling started");

        debugDialog.show();

        glfwSetCharCallback(glfwWindow, addResource(new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                if (keyLogging) {
                    keyLoggingText.appendCodePoint(codepoint);
                }
            }
        }));
        glfwSetFramebufferSizeCallback(glfwWindow, addResource(new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                EventHandler.width = width;
                EventHandler.height = height;

                GL46.glViewport(0, 0, width, height);
            }
        }));
    }

    public static void startKeyLogging() {
        keyLogging = true;
    }

    public static void endKeyLogging() {
        resetKeyLogginText();
        keyLogging = false;
    }

    public static boolean isMousePressed(Element element) {
        return getRectangleClick(element.x(), element.y(), element.x() + element.width(), element.y() + element.height());
    }

    public static boolean getRectangleClick(float x, float y, float x1, float y1) {
        Point2i mousePos = input.mousePos();

        return mousePos.x >= x && mousePos.x <= x1 &&
                mousePos.y >= y && mousePos.y <= y1 &&
                input.justClicked(GLFW_MOUSE_BUTTON_LEFT);
    }

    private static void updateHotkeys() {
        if (start) {
            if (input.justPressed(GLFW_KEY_ESCAPE)) {
                UI.pause().toggle();
            }

            if (!windowFocused && Config.getFromConfig("Autopause").equals("true")) {
                UI.pause().show();
                Physics.stopPhysics();
            }
        }

        if ((input.justPressed(GLFW_KEY_BACKSPACE) || input.repeated(GLFW_KEY_BACKSPACE)) && isKeylogging()) {
            int length = keyLoggingText.length();

            if (length > 0) {
                keyLoggingText.deleteCharAt(length - 1);
            }
        }
    }

    public static void setDebugValue(Supplier<String> format) {
        if (debugLevel > 0) {
            var elem = new DebugBox(format);

            debugDialog.add(elem);
            int i = debugDialog.children().size();
            elem.setPosition(5, 1080 - (25 * i));
        }
    }

    public static void init() {
        initCallbacks();

        setDebugValue(() -> "[Render] fps: " + Global.app.getFps());
    }

    public static void update() {
        Player.updatePlayerGUILogic();
        updateHotkeys();
        updateLine();
    }

    public static boolean isKeylogging() {
        return keyLogging;
    }

    public static void resetKeyLogginText() {
        keyLoggingText.setLength(0);
    }
}
