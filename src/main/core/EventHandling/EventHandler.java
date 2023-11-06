package core.EventHandling;

import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Json;
import core.EventHandling.Logging.Logger;
import core.UI.GUI.CreateElement;
import core.UI.GUI.Menu.*;
import core.UI.GUI.Objects.ButtonObject;
import core.UI.GUI.Objects.SliderObject;
import core.World.Creatures.Player.Player;
import core.Utils.SimpleColor;
import core.World.WorldGenerator;
import org.lwjgl.glfw.*;
import java.awt.Point;
import static core.Utils.Commandline.updateLine;
import static core.EventHandling.Logging.Logger.log;
import static core.UI.GUI.CreateElement.*;
import static core.Window.*;
import static core.World.Creatures.Physics.updates;
import static org.lwjgl.glfw.GLFW.*;

public class EventHandler extends Thread {
    public static long lastMouseMovedTime = System.currentTimeMillis();
    private static long lastSecond = System.currentTimeMillis();
    private static final Point lastMousePos = new Point(0, 0);
    private static boolean keyLogging = false;
    public static String keyLoggingText = "";
    private static final boolean[] pressedButtons = new boolean[349];
    private static int handlerUpdates = 0;
    public static int width, height; // TODO: scaling

    public EventHandler() {
        log("Thread: Event handling started");

        glfwSetCursorPosCallback(glfwWindow, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                double mouseX = xpos * MouseCalibration.xMultiplier;
                double mouseY = ypos / MouseCalibration.yMultiplier;
                double invertedY = height - mouseY;

                lastMouseMovedTime = System.currentTimeMillis();
                lastMousePos.setLocation(mouseX, invertedY);
            }
        });
        glfwSetWindowSizeCallback(glfwWindow, new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                EventHandler.width = width;
                EventHandler.height = height;
            }
        });
        glfwSetKeyCallback(glfwWindow, new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW.GLFW_KEY_F4 && mods == GLFW.GLFW_MOD_ALT) {
                    Logger.logExit(1863);
                }
            }
        });
    }

    public static void startKeyLogging() {
        keyLogging = true;
    }

    public static void endKeyLogging() {
        keyLoggingText = "";
        keyLogging = false;
    }

    public static Point getMousePos() {
        return lastMousePos;
    }

    public static boolean getKey(int key) {
        return glfwGetKey(glfwWindow, key) == 1;
    }

    public static boolean getKeyClick(int key) {
        if (!pressedButtons[key] && glfwGetKey(glfwWindow, key) == 1) {
            pressedButtons[key] = true;
            return true;
        } else if (!getKey(key)) {
            pressedButtons[key] = false;
            return false;
        }

        return false;
    }

    public static boolean getMousePress() {
        return glfwGetMouseButton(glfwWindow, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
    }

    public static boolean getRectanglePress(int x, int y, int x1, int y1) {
        Point mousePos = getMousePos();

        return mousePos.x >= x && mousePos.x <= x1 && mousePos.y >= y && mousePos.y <= y1 && getMousePress();
    }

    private static void updateSliders() {
        for (SliderObject slider : sliders.values()) {
            if (!slider.visible) {
                slider.isClicked = false;
                continue;
            }

            if (EventHandler.getRectanglePress(slider.x, slider.y, slider.width + slider.x, slider.height + slider.y)) {
                slider.sliderPos = EventHandler.getMousePos().x;
            }
        }
    }

    private static void updateButtons() {
        for (ButtonObject button : buttons.values()) {
            if (!button.visible || !button.isClickable) {
                button.isClicked = false;
                continue;
            }

            if (button.swapButton) {
                if (System.currentTimeMillis() - button.lastClickTime >= 150 && EventHandler.getRectanglePress(button.x, button.y, button.width + button.x, button.height + button.y)) {
                    button.isClicked = !button.isClicked;
                    button.lastClickTime = System.currentTimeMillis();
                }
            } else {
                boolean press = EventHandler.getRectanglePress(button.x, button.y, button.width + button.x, button.height + button.y);
                button.isClicked = press;

                if (press && button.taskOnClick != null) {
                   button.taskOnClick.run();
                }
            }
        }
    }

    private static void updateKeyLogging() {
        if (keyLogging) {
            for (int i = 48; i <= 90; i++) {
                if (getKeyClick(GLFW_KEY_SPACE)) {
                    keyLoggingText += " ";
                }
                if (getKeyClick(GLFW_KEY_BACKSPACE)) {
                    if (keyLoggingText.length() > 0) {
                        keyLoggingText = keyLoggingText.substring(0, keyLoggingText.length() - 1);
                    }
                }
                if (getKeyClick(GLFW_KEY_PERIOD)) {
                    keyLoggingText += ".";
                }

                //a - z, 0 - 9
                if (i <= 57 || i >= 65) {
                    if (getKeyClick(i)) {
                        keyLoggingText += !getKey(GLFW_KEY_LEFT_SHIFT) ? glfwGetKeyName(i, 0) : glfwGetKeyName(i, 0).toUpperCase();
                    }
                }
            }
        }
    }

    private static void updateClicks() {
        if (Settings.createdSettings && !buttons.get(Json.getName("SettingsSave")).isClicked) {
            int count = (int) buttons.values().stream().filter(currentButton -> currentButton.isClicked && currentButton.visible && (currentButton.group.contains("Swap") || currentButton.group.contains("Drop"))).count();

            if (Settings.needUpdateCount) {
                Settings.pressedCount = count;
                Settings.needUpdateCount = false;
            } else if (count != Settings.pressedCount) {
                buttons.get(Json.getName("SettingsSave")).isClickable = true;
            }
        }

        if (sliders.get("worldSize") != null && sliders.get("worldSize").visible) {
            float worldSize = sliders.get("worldSize").max;
            String pic;

            if (getSliderPos("worldSize") >= worldSize / 1.5f) {
                pic = "planetBig.png";
            } else if (getSliderPos("worldSize") >= worldSize / 3) {
                pic = "planetAverage.png";
            } else {
                pic = "planetMini.png";
            }
            panels.get("planet").options = assetsDir("World/WorldGenerator/" + pic);
        }
    }

    private static void updateHotkeys() {
        if (getKeyClick(GLFW_KEY_ESCAPE) && start) {
            if (!Pause.created) {
                Pause.create();
            } else {
                Pause.delete();
            }
            Settings.delete();
        }
    }

    private static void updateDebug() {
        if (Integer.parseInt(Config.getFromConfig("Debug")) > 0 && System.currentTimeMillis() - lastSecond >= 1000) {
            lastSecond = System.currentTimeMillis();

            if (start) {
                CreateElement.createText(5, 980, "PlayerPos", "Player pos: x - " + (int) WorldGenerator.DynamicObjects.get(0).x + "(" + (int) WorldGenerator.DynamicObjects.get(0).x / 16 + ") y - " + (int) WorldGenerator.DynamicObjects.get(0).y + "(" + (int) WorldGenerator.DynamicObjects.get(0).y / 16 + ")", new SimpleColor(25, 25, 25, 255), null);
                CreateElement.createText(5, 1005, "PhysicsFPS", "Physics FPS: " + updates, new SimpleColor(25, 25, 25, 255), null);
            }
            CreateElement.createText(5, 1030, "HandlerFPS", "Handler FPS: " + handlerUpdates, new SimpleColor(25, 25, 25, 255), null);
            CreateElement.createText(5, 1055, "GameFPS", "Game FPS: " + fps, new SimpleColor(25, 25, 25, 255), null);

            handlerUpdates = 0;
            updates = 0;
            fps = 0;
        }
    }

    @Override
    public void run() {
        while (!glfwWindowShouldClose(glfwWindow)) {
            Player.updatePlayerGUILogic();
            updateButtons();
            updateClicks();
            updateKeyLogging();
            updateSliders();
            updateHotkeys();
            updateLine();
            updateDebug();

            handlerUpdates++;
        }
    }
}

/*
all button are public static final int

            GLFW_KEY_SPACE         = 32,
            GLFW_KEY_APOSTROPHE    = 39,
            GLFW_KEY_COMMA         = 44,
            GLFW_KEY_MINUS         = 45,
            GLFW_KEY_PERIOD        = 46,
            GLFW_KEY_SLASH         = 47,
            GLFW_KEY_0             = 48,
            GLFW_KEY_1             = 49,
            GLFW_KEY_2             = 50,
            GLFW_KEY_3             = 51,
            GLFW_KEY_4             = 52,
            GLFW_KEY_5             = 53,
            GLFW_KEY_6             = 54,
            GLFW_KEY_7             = 55,
            GLFW_KEY_8             = 56,
            GLFW_KEY_9             = 57,
            GLFW_KEY_SEMICOLON     = 59,
            GLFW_KEY_EQUAL         = 61,
            GLFW_KEY_A             = 65,
            GLFW_KEY_B             = 66,
            GLFW_KEY_C             = 67,
            GLFW_KEY_D             = 68,
            GLFW_KEY_E             = 69,
            GLFW_KEY_F             = 70,
            GLFW_KEY_G             = 71,
            GLFW_KEY_H             = 72,
            GLFW_KEY_I             = 73,
            GLFW_KEY_J             = 74,
            GLFW_KEY_K             = 75,
            GLFW_KEY_L             = 76,
            GLFW_KEY_M             = 77,
            GLFW_KEY_N             = 78,
            GLFW_KEY_O             = 79,
            GLFW_KEY_P             = 80,
            GLFW_KEY_Q             = 81,
            GLFW_KEY_R             = 82,
            GLFW_KEY_S             = 83,
            GLFW_KEY_T             = 84,
            GLFW_KEY_U             = 85,
            GLFW_KEY_V             = 86,
            GLFW_KEY_W             = 87,
            GLFW_KEY_X             = 88,
            GLFW_KEY_Y             = 89,
            GLFW_KEY_Z             = 90,
            GLFW_KEY_LEFT_BRACKET  = 91,
            GLFW_KEY_BACKSLASH     = 92,
            GLFW_KEY_RIGHT_BRACKET = 93,
            GLFW_KEY_GRAVE_ACCENT  = 96,
            GLFW_KEY_WORLD_1       = 161,
            GLFW_KEY_WORLD_2       = 162;

            GLFW_KEY_ESCAPE        = 256,
            GLFW_KEY_ENTER         = 257,
            GLFW_KEY_TAB           = 258,
            GLFW_KEY_BACKSPACE     = 259,
            GLFW_KEY_INSERT        = 260,
            GLFW_KEY_DELETE        = 261,
            GLFW_KEY_RIGHT         = 262,
            GLFW_KEY_LEFT          = 263,
            GLFW_KEY_DOWN          = 264,
            GLFW_KEY_UP            = 265,
            GLFW_KEY_PAGE_UP       = 266,
            GLFW_KEY_PAGE_DOWN     = 267,
            GLFW_KEY_HOME          = 268,
            GLFW_KEY_END           = 269,
            GLFW_KEY_CAPS_LOCK     = 280,
            GLFW_KEY_SCROLL_LOCK   = 281,
            GLFW_KEY_NUM_LOCK      = 282,
            GLFW_KEY_PRINT_SCREEN  = 283,
            GLFW_KEY_PAUSE         = 284,
            GLFW_KEY_F1            = 290,
            GLFW_KEY_F2            = 291,
            GLFW_KEY_F3            = 292,
            GLFW_KEY_F4            = 293,
            GLFW_KEY_F5            = 294,
            GLFW_KEY_F6            = 295,
            GLFW_KEY_F7            = 296,
            GLFW_KEY_F8            = 297,
            GLFW_KEY_F9            = 298,
            GLFW_KEY_F10           = 299,
            GLFW_KEY_F11           = 300,
            GLFW_KEY_F12           = 301,
            GLFW_KEY_F13           = 302,
            GLFW_KEY_F14           = 303,
            GLFW_KEY_F15           = 304,
            GLFW_KEY_F16           = 305,
            GLFW_KEY_F17           = 306,
            GLFW_KEY_F18           = 307,
            GLFW_KEY_F19           = 308,
            GLFW_KEY_F20           = 309,
            GLFW_KEY_F21           = 310,
            GLFW_KEY_F22           = 311,
            GLFW_KEY_F23           = 312,
            GLFW_KEY_F24           = 313,
            GLFW_KEY_F25           = 314,
            GLFW_KEY_KP_0          = 320,
            GLFW_KEY_KP_1          = 321,
            GLFW_KEY_KP_2          = 322,
            GLFW_KEY_KP_3          = 323,
            GLFW_KEY_KP_4          = 324,
            GLFW_KEY_KP_5          = 325,
            GLFW_KEY_KP_6          = 326,
            GLFW_KEY_KP_7          = 327,
            GLFW_KEY_KP_8          = 328,
            GLFW_KEY_KP_9          = 329,
            GLFW_KEY_KP_DECIMAL    = 330,
            GLFW_KEY_KP_DIVIDE     = 331,
            GLFW_KEY_KP_MULTIPLY   = 332,
            GLFW_KEY_KP_SUBTRACT   = 333,
            GLFW_KEY_KP_ADD        = 334,
            GLFW_KEY_KP_ENTER      = 335,
            GLFW_KEY_KP_EQUAL      = 336,
            GLFW_KEY_LEFT_SHIFT    = 340,
            GLFW_KEY_LEFT_CONTROL  = 341,
            GLFW_KEY_LEFT_ALT      = 342,
            GLFW_KEY_LEFT_SUPER    = 343,
            GLFW_KEY_RIGHT_SHIFT   = 344,
            GLFW_KEY_RIGHT_CONTROL = 345,
            GLFW_KEY_RIGHT_ALT     = 346,
            GLFW_KEY_RIGHT_SUPER   = 347,
            GLFW_KEY_MENU          = 348,
            GLFW_KEY_LAST          = GLFW_KEY_MENU;


            GLFW_MOD_SHIFT = 0x1;
            GLFW_MOD_CONTROL = 0x2;
            GLFW_MOD_ALT = 0x4;
            GLFW_MOD_SUPER = 0x8;
            GLFW_MOD_CAPS_LOCK = 0x10;
            GLFW_MOD_NUM_LOCK = 0x20;

            GLFW_MOUSE_BUTTON_1      = 0,
            GLFW_MOUSE_BUTTON_2      = 1,
            GLFW_MOUSE_BUTTON_3      = 2,
            GLFW_MOUSE_BUTTON_4      = 3,
            GLFW_MOUSE_BUTTON_5      = 4,
            GLFW_MOUSE_BUTTON_6      = 5,
            GLFW_MOUSE_BUTTON_7      = 6,
            GLFW_MOUSE_BUTTON_8      = 7,
            GLFW_MOUSE_BUTTON_LAST   = GLFW_MOUSE_BUTTON_8,
            GLFW_MOUSE_BUTTON_LEFT   = GLFW_MOUSE_BUTTON_1,
            GLFW_MOUSE_BUTTON_RIGHT  = GLFW_MOUSE_BUTTON_2,
            GLFW_MOUSE_BUTTON_MIDDLE = GLFW_MOUSE_BUTTON_3;

            GLFW_JOYSTICK_1    = 0,
            GLFW_JOYSTICK_2    = 1,
            GLFW_JOYSTICK_3    = 2,
            GLFW_JOYSTICK_4    = 3,
            GLFW_JOYSTICK_5    = 4,
            GLFW_JOYSTICK_6    = 5,
            GLFW_JOYSTICK_7    = 6,
            GLFW_JOYSTICK_8    = 7,
            GLFW_JOYSTICK_9    = 8,
            GLFW_JOYSTICK_10   = 9,
            GLFW_JOYSTICK_11   = 10,
            GLFW_JOYSTICK_12   = 11,
            GLFW_JOYSTICK_13   = 12,
            GLFW_JOYSTICK_14   = 13,
            GLFW_JOYSTICK_15   = 14,
            GLFW_JOYSTICK_16   = 15,
            GLFW_JOYSTICK_LAST = GLFW_JOYSTICK_16;

            GLFW_GAMEPAD_BUTTON_A            = 0,
            GLFW_GAMEPAD_BUTTON_B            = 1,
            GLFW_GAMEPAD_BUTTON_X            = 2,
            GLFW_GAMEPAD_BUTTON_Y            = 3,
            GLFW_GAMEPAD_BUTTON_LEFT_BUMPER  = 4,
            GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER = 5,
            GLFW_GAMEPAD_BUTTON_BACK         = 6,
            GLFW_GAMEPAD_BUTTON_START        = 7,
            GLFW_GAMEPAD_BUTTON_GUIDE        = 8,
            GLFW_GAMEPAD_BUTTON_LEFT_THUMB   = 9,
            GLFW_GAMEPAD_BUTTON_RIGHT_THUMB  = 10,
            GLFW_GAMEPAD_BUTTON_DPAD_UP      = 11,
            GLFW_GAMEPAD_BUTTON_DPAD_RIGHT   = 12,
            GLFW_GAMEPAD_BUTTON_DPAD_DOWN    = 13,
            GLFW_GAMEPAD_BUTTON_DPAD_LEFT    = 14,
            GLFW_GAMEPAD_BUTTON_LAST         = GLFW_GAMEPAD_BUTTON_DPAD_LEFT,
            GLFW_GAMEPAD_BUTTON_CROSS        = GLFW_GAMEPAD_BUTTON_A,
            GLFW_GAMEPAD_BUTTON_CIRCLE       = GLFW_GAMEPAD_BUTTON_B,
            GLFW_GAMEPAD_BUTTON_SQUARE       = GLFW_GAMEPAD_BUTTON_X,
            GLFW_GAMEPAD_BUTTON_TRIANGLE     = GLFW_GAMEPAD_BUTTON_Y;

            GLFW_GAMEPAD_AXIS_LEFT_X        = 0,
            GLFW_GAMEPAD_AXIS_LEFT_Y        = 1,
            GLFW_GAMEPAD_AXIS_RIGHT_X       = 2,
            GLFW_GAMEPAD_AXIS_RIGHT_Y       = 3,
            GLFW_GAMEPAD_AXIS_LEFT_TRIGGER  = 4,
            GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER = 5,
            GLFW_GAMEPAD_AXIS_LAST          = GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER;
            */
