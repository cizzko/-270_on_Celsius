package core.EventHandling;

import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Json;
import core.UI.GUI.CreateElement;
import core.UI.GUI.Menu.Pause;
import core.UI.GUI.Menu.Settings;
import core.UI.GUI.Objects.ButtonObject;
import core.UI.GUI.Objects.SliderObject;
import core.Utils.SimpleColor;
import core.Utils.SimpleLongSummaryStatistics;
import core.Window;
import core.World.Creatures.Player.Player;
import core.math.Point2i;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.opengl.GL46;

import java.util.HashMap;
import java.util.Map;

import static core.EventHandling.Logging.Logger.log;
import static core.Global.*;
import static core.UI.GUI.CreateElement.*;
import static core.Utils.Commandline.updateLine;
import static core.Window.*;
import static org.lwjgl.glfw.GLFW.*;

public class EventHandler {
    private static long lastSecond = System.currentTimeMillis();
    private static boolean keyLogging = false;
    public static final StringBuilder keyLoggingText = new StringBuilder(256);

    public static int width = defaultWidth, height = defaultHeight;
    private static HashMap<String, debugValue> debugValues = new HashMap<>();
    private record debugValue(boolean statistics, String text, SimpleLongSummaryStatistics summaryStatistics) {}

    public static void setKeyLoggingText(String text) {
        keyLoggingText.setLength(0);
        keyLoggingText.append(text);
    }

    private static void initCallbacks() {
        log("Thread: Event handling started");

        glfwSetCharCallback(glfwWindow, Window.addResource(new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                if (keyLogging) {
                    keyLoggingText.appendCodePoint(codepoint);
                }
            }
        }));
        glfwSetFramebufferSizeCallback(glfwWindow, Window.addResource(new GLFWFramebufferSizeCallback() {
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

    public static boolean getRectanglePress(int x, int y, int x1, int y1) {
        Point2i mousePos = input.mousePos();

        return mousePos.x >= x && mousePos.x <= x1 && mousePos.y >= y && mousePos.y <= y1 &&
                input.justClicked(GLFW_MOUSE_BUTTON_LEFT);
    }

    private static void updateSliders() {
        Point2i mousePos = input.mousePos();

        for (SliderObject slider : sliders.values()) {
            if (!slider.visible) {
                slider.isClicked = false;
                continue;
            }

            if (slider.contains(mousePos) && input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
                slider.sliderPos = mousePos.x;
            }
        }
    }

    private static void updateButtons() {
        for (ButtonObject button : buttons.values()) {
            if (button == null || !button.visible || !button.isClickable) {
                continue;
            }

            if (button.swapButton) {
                if (System.currentTimeMillis() - button.lastClickTime >= 150 &&
                        EventHandler.getRectanglePress(button.x, button.y, button.width + button.x, button.height + button.y)) {
                    button.isClicked = !button.isClicked;
                    button.lastClickTime = System.currentTimeMillis();
                }
            } else {
                boolean press = EventHandler.getRectanglePress(button.x, button.y, button.width + button.x, button.height + button.y);
                button.isClicked = press;

                if (press && button.taskOnClick != null) {
                    Thread.startVirtualThread(() -> button.taskOnClick.run());
                    return;
                }
            }
        }
    }

    private static void updateClicks() {
        if (Settings.createdSettings && !buttons.get(Json.getName("SettingsSave")).isClicked) {
            int count = (int) buttons.values().stream().filter(currentButton -> currentButton.isClicked &&
                    currentButton.visible && (currentButton.group.contains("Swap") || currentButton.group.contains("Drop"))).count();

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

            int sliderPos = getSliderPos("worldSize");
            if (sliderPos >= worldSize / 1.5f) {
                pic = "planetBig.png";
            } else if (sliderPos >= worldSize / 3) {
                pic = "planetAverage.png";
            } else {
                pic = "planetMini.png";
            }
            panels.get("planet").texture = atlas.byPath("World/WorldGenerator/" + pic);
        }
    }

    private static void updateHotkeys() {
        if (input.justPressed(GLFW_KEY_ESCAPE) && start) {
            if (!Pause.created) {
                Pause.create();
            } else {
                Pause.delete();
            }
            Settings.delete();
        }
        if (input.justPressed(GLFW_KEY_BACKSPACE) && isKeylogging()) {
            int length = keyLoggingText.length();
            if (length > 0) {
                keyLoggingText.deleteCharAt(length - 1);
            }
        }
    }

    private static void updateDebug() {
        if (System.currentTimeMillis() - lastSecond >= 1000 && Integer.parseInt(Config.getFromConfig("Debug")) > 0) {
            lastSecond = System.currentTimeMillis();

            int iterations = 0;
            for (Map.Entry<String, debugValue> iteration : debugValues.entrySet()) {
                debugValue value = iteration.getValue();
                String key = iteration.getKey();

                iterations++;
                if (value.statistics) {
                    CreateElement.createText(5, 1080 - (25 * iterations), key, value.text + value.summaryStatistics.toString(), SimpleColor.DIRTY_BRIGHT_BLACK, null);
                } else {
                    CreateElement.createText(5, 1080 - (25 * iterations), key, value.text, SimpleColor.DIRTY_BRIGHT_BLACK, null);
                }
            }
        }
    }

    public static void addDebugValue(boolean statistics, String text, String name) {
        debugValue object = debugValues.getOrDefault(name, null);

        if (object == null) {
            debugValues.put(name, new debugValue(statistics, text, statistics ? new SimpleLongSummaryStatistics() : null));
        } else if (statistics) {
            object.summaryStatistics.add(1);
        }
    }

    public static void putDebugValue(boolean statistics, String text, String name) {
        debugValues.put(name, new debugValue(statistics, text, statistics ? new SimpleLongSummaryStatistics() : null));
    }

    public static void init() {
        initCallbacks();
    }

    public static void update() {
        Player.updatePlayerGUILogic();
        updateButtons();
        updateClicks();
        updateSliders();
        updateHotkeys();
        updateLine();
        updateDebug();

        addDebugValue(true, "Handler fps: ", "HandlerFPS");
    }

    public static boolean isKeylogging() {
        return keyLogging;
    }

    public static void resetKeyLogginText() {
        keyLoggingText.setLength(0);
    }
}
