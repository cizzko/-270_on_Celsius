package core.EventHandling;

import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Json;
import core.EventHandling.Logging.Logger;
import core.Global;
import core.UI.GUI.CreateElement;
import core.UI.GUI.Menu.*;
import core.UI.GUI.Objects.ButtonObject;
import core.UI.GUI.Objects.SliderObject;
import core.World.Creatures.Player.Player;
import core.Utils.SimpleColor;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import org.lwjgl.glfw.*;

import java.awt.Point;

import static core.Utils.Commandline.updateLine;
import static core.EventHandling.Logging.Logger.log;
import static core.UI.GUI.CreateElement.*;
import static core.Window.*;
import static core.World.Creatures.Physics.updates;
import static org.lwjgl.glfw.GLFW.*;

public class EventHandler {
    private static long lastSecond = System.currentTimeMillis();
    private static boolean keyLogging = false;
    public static String keyLoggingText = "";
    private static int handlerUpdates = 0;
    public static int width, height; // TODO: scaling

    private static void initCallbacks() {
        log("Thread: Event handling started");

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

    public static boolean getRectanglePress(int x, int y, int x1, int y1) {
        Point mousePos = Global.input.mousePos();

        return mousePos.x >= x && mousePos.x <= x1 && mousePos.y >= y && mousePos.y <= y1 && Global.input.justClicked(GLFW_MOUSE_BUTTON_LEFT);
    }

    private static void updateSliders() {
        for (SliderObject slider : sliders.values()) {
            if (!slider.visible) {
                slider.isClicked = false;
                continue;
            }

            if (EventHandler.getRectanglePress(slider.x, slider.y, slider.width + slider.x, slider.height + slider.y)) {
                slider.sliderPos = Global.input.mousePos().x;
            }
        }
    }

    private static void updateButtons() {
        for (ButtonObject button : buttons.values()) {
            if (button == null || !button.visible || !button.isClickable) {
                if (button != null) {
                    button.isClicked = false;
                }
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
                if (Global.input.pressed(GLFW_KEY_SPACE)) {
                    keyLoggingText += " ";
                }
                if (Global.input.pressed(GLFW_KEY_BACKSPACE)) {
                    if (keyLoggingText.length() > 0) {
                        keyLoggingText = keyLoggingText.substring(0, keyLoggingText.length() - 1);
                    }
                }
                if (Global.input.pressed(GLFW_KEY_PERIOD)) {
                    keyLoggingText += ".";
                }

                //a - z, 0 - 9
                if (i <= 57 || i >= 65) {
                    if (Global.input.pressed(i)) {
                        keyLoggingText += !Global.input.pressed(GLFW_KEY_LEFT_SHIFT) ? glfwGetKeyName(i, 0) : glfwGetKeyName(i, 0).toUpperCase();
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
        if (Global.input.pressed(GLFW_KEY_ESCAPE) && start) {
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
                CreateElement.createText(5, 980, "PlayerPos", "Player pos: x - " + (int) WorldGenerator.DynamicObjects.getFirst().getX() + "(" + (int) WorldGenerator.DynamicObjects.getFirst().getX() / TextureDrawing.blockSize + ") y - " + (int) WorldGenerator.DynamicObjects.getFirst().getX() + "(" + (int) WorldGenerator.DynamicObjects.getFirst().getY() / TextureDrawing.blockSize + ")", new SimpleColor(25, 25, 25, 255), null);
                CreateElement.createText(5, 1005, "PhysicsFPS", "Physics FPS: " + updates, new SimpleColor(25, 25, 25, 255), null);
            }
            CreateElement.createText(5, 1030, "HandlerFPS", "Handler FPS: " + handlerUpdates, new SimpleColor(25, 25, 25, 255), null);
            CreateElement.createText(5, 1055, "GameFPS", "Game FPS: " + fps, new SimpleColor(25, 25, 25, 255), null);

            handlerUpdates = 0;
            updates = 0;
            fps = 0;
        }
    }

    public static void init() {
        initCallbacks();
    }

    public static void update() {
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
