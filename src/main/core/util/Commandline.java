package core.util;

import core.EventHandling.EventHandler;
import core.EventHandling.Config;
import core.Window;
import core.g2d.Fill;

import static core.EventHandling.EventHandler.keyLoggingText;
import static core.Global.input;
import static core.World.Textures.TextureDrawing.drawRectangleText;
import static org.lwjgl.glfw.GLFW.*;

public class Commandline {
    private static final String prefix = Config.getFromFC("Prefix");
    public static boolean created = false;

    private static void startReflection(String target) {
        JavaInterpreter.execute(target);
    }

    public static void createLine() {
        EventHandler.startKeyLogging();
        EventHandler.resetKeyLogginText();
        created = true;
    }

    public static void deleteLine() {
        EventHandler.endKeyLogging();
        created = false;
    }

    public static void inputUpdate() {
        if (input.justPressed(GLFW_KEY_F5)) {
            if (created) {
                Commandline.deleteLine();
            } else {
                Commandline.createLine();
            }
        }
        if (input.justPressed(GLFW_KEY_DELETE)) {
            EventHandler.resetKeyLogginText();
        }

        if (created) {
            if (input.justPressed(GLFW_KEY_ENTER)) {
                startReflection(keyLoggingText.toString());
            }

            if (input.pressed(GLFW_KEY_LEFT_CONTROL) && input.justPressed(GLFW_KEY_V)) {
                String text = glfwGetClipboardString(Window.glfwWindow);
                if (text != null) {
                    keyLoggingText.append(text);
                }
            }
        }
    }

    public static void draw() {
        if (created) {
            Fill.rect(20, 800, 650, 260, Color.rgba8888(0, 0, 0, 220));
            drawRectangleText(-10, 810, 630, EventHandler.keyLoggingText.toString(), true, Color.CLEAR, Window.defaultFont);
        }
    }
}
