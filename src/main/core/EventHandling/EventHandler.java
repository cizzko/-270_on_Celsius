package core.EventHandling;

import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Json;
import core.EventHandling.Logging.Logger;
import core.UI.GUI.CreateElement;
import core.UI.GUI.Menu.Pause;
import core.UI.GUI.Menu.Settings;
import core.UI.GUI.Objects.ButtonObject;
import core.UI.GUI.Objects.SliderObject;
import core.Utils.SimpleColor;
import core.Window;
import core.World.Creatures.Player.Player;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import core.math.Rectangle;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL46;

import static core.EventHandling.Logging.Logger.log;
import static core.Global.*;
import static core.UI.GUI.CreateElement.*;
import static core.Utils.Commandline.updateLine;
import static core.Window.*;
import static core.World.Creatures.Physics.updates;
import static org.lwjgl.glfw.GLFW.*;

public class EventHandler {
    private static long lastSecond = System.currentTimeMillis();
    private static boolean keyLogging = false;
    public static final StringBuilder keyLoggingText = new StringBuilder(256);
    private static int handlerUpdates = 0;

    public static int width = defaultWidth, height = defaultHeight;

    public static void setKeyLoggingText(String text) {
        keyLoggingText.setLength(0);
        keyLoggingText.append(text);
    }

    private static void initCallbacks() {
        log("Thread: Event handling started");

        glfwSetCharCallback(glfwWindow, Window.addResource(new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                if (keyLogging)
                    keyLoggingText.appendCodePoint(codepoint);
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
        glfwSetKeyCallback(glfwWindow, Window.addResource(new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW.GLFW_KEY_F4 && mods == GLFW.GLFW_MOD_ALT) {
                    Logger.logExit(1863);
                }
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
        var mousePos = input.mousePos();

        return mousePos.x >= x && mousePos.x <= x1 && mousePos.y >= y && mousePos.y <= y1 &&
                input.justClicked(GLFW_MOUSE_BUTTON_LEFT);
    }

    private static void updateSliders() {
        var mousePos = input.mousePos();

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
    }

    private static void updateDebug() {
        if (Integer.parseInt(Config.getFromConfig("Debug")) > 0 && System.currentTimeMillis() - lastSecond >= 1000) {
            lastSecond = System.currentTimeMillis();

            if (start) {
                CreateElement.createText(5, 980, "PlayerPos", "Player pos: x - " + (int) WorldGenerator.DynamicObjects.getFirst().getX() + "(" + (int) WorldGenerator.DynamicObjects.getFirst().getX() / TextureDrawing.blockSize + ") y - " + (int) WorldGenerator.DynamicObjects.getFirst().getX() + "(" + (int) WorldGenerator.DynamicObjects.getFirst().getY() / TextureDrawing.blockSize + ")", SimpleColor.DIRTY_BRIGHT_BLACK, null);
                CreateElement.createText(5, 1005, "PhysicsFPS", "Physics FPS: " + updates, SimpleColor.DIRTY_BRIGHT_BLACK, null);
            }
            CreateElement.createText(5, 1030, "HandlerFPS", "Handler FPS: " + handlerUpdates, SimpleColor.DIRTY_BRIGHT_BLACK, null);
            CreateElement.createText(5, 1055, "GameFPS", "Game FPS: " + fps, SimpleColor.DIRTY_BRIGHT_BLACK, null);

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
        updateSliders();
        updateHotkeys();
        updateLine();
        updateDebug();

        handlerUpdates++;
    }

    public static boolean isKeylogging() {
        return keyLogging;
    }

    public static void resetKeyLogginText() {
        keyLoggingText.setLength(0);
    }
}
