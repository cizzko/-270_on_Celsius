package core.EventHandling;

import core.EventHandling.Logging.Config;
import core.Global;
import core.PlayGameScene;
import core.UI;
import core.graphic.Layer;
import core.ui.Dialog;
import core.math.Point2i;
import core.ui.Element;
import core.ui.Styles;
import core.ui.TextArea;
import org.lwjgl.glfw.GLFWCharCallback;

import java.util.function.Supplier;

import static core.Global.*;
import static core.Window.*;
import static org.lwjgl.glfw.GLFW.*;

public class EventHandler {
    private static boolean keyLogging = false;
    public static final StringBuilder keyLoggingText = new StringBuilder(256);
    public static int debugLevel = Integer.parseInt(Config.getFromConfig("Debug"));

    private static final class DebugBox extends TextArea {
        private final Supplier<String> format;

        private DebugBox(Supplier<String> format) {
            super(debugDialog, Styles.DEBUG_TEXT);
            this.format = format;
        }

        @Override
        public void updateThis() {
            setText(format.get());
        }

        @Override
        public void draw() {
            if (!visible()) {
                return;
            }
            batch.draw(Layer.DEBUG, () -> super.draw());
        }
    }
    private static final Dialog debugDialog = new Dialog();

    public static void setKeyLoggingText(String text) {
        keyLoggingText.setLength(0);
        keyLoggingText.append(text);
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

    public static void updateHotkeys(PlayGameScene scene) {
        if (input.justPressed(GLFW_KEY_ESCAPE)) {
            scene.togglePaused();
            UI.pause().toggle();
        }

        if (!windowFocused && Config.getFromConfig("Autopause").equals("true")) {
            scene.setPaused(true);
            UI.pause().show();
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
        debugDialog.show();

        glfwSetCharCallback(glfwWindow, Global.app.keep(new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                if (keyLogging) {
                    keyLoggingText.appendCodePoint(codepoint);
                }
            }
        }));

        setDebugValue(() -> "[Render] fps: " + Global.app.getFps());
    }

    public static boolean isKeylogging() {
        return keyLogging;
    }

    public static void resetKeyLogginText() {
        keyLoggingText.setLength(0);
    }
}
