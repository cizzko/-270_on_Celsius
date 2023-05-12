package core.GUI;

import core.EventHandling.EventHandler;
import core.GUI.objects.ButtonObject;
import core.GUI.objects.PanelObject;
import core.GUI.objects.SliderObject;
import core.Logging.json;
import core.Logging.logger;
import core.Menu.MainMenu;
import core.Window;
import core.World.creatures.CreaturesGenerate;
import core.World.creatures.Physics;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static core.EventHandling.EventHandler.getKey;
import static core.Window.defPath;
import static core.Window.glfwWindow;
import static org.lwjgl.glfw.GLFW.*;

public class CreateElement extends Thread {
    public static ConcurrentHashMap<String, ButtonObject> buttons = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, SliderObject> sliders = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, PanelObject> panels = new ConcurrentHashMap<>();

    public static void createButton(int x, int y, int width, int height, String name, Boolean visible, Color color) {
        buttons.put(name, new ButtonObject(visible, x, y, height, width, name, color));
    }

    public static void createSlider(int x, int y, int width, int height, int max, String name, Boolean visible) {
        sliders.put(name, new SliderObject(visible, x, y, width, height, max));
        sliders.get(name).sliderPos = x + 1;
    }

    public static void createPanel(int x, int y, int width, int height, String name, boolean visible, boolean simple) {
        panels.put(name, new PanelObject(x, y, width, height, name, visible, simple));
    }

    @Override
    public void run() {
        while (!glfwWindowShouldClose(glfwWindow)) {
            updateKeyShortcut();
            updateButtons();
            updateSliders();
        }
    }

    public static void updateSliders() {
        for (Map.Entry<String, SliderObject> entry : sliders.entrySet()) {
            String slider = entry.getKey();

            if (!sliders.get(slider).visible) {
                sliders.get(slider).isClicked = false;
                continue;
            }
            if (EventHandler.getRectangleClick(sliders.get(slider).x, sliders.get(slider).y, sliders.get(slider).width,sliders.get(slider).height + sliders.get(slider).y)) {
                sliders.get(slider).sliderPos = EventHandler.getMousePos().x;
            }
        }
    }

    public static void updateButtons() {
        for (Map.Entry<String, ButtonObject> entry : buttons.entrySet()) {
            String button = entry.getKey();
            if (!buttons.get(button).visible) {
                buttons.get(button).isClicked = false;
                continue;
            }

            buttons.get(button).isClicked = EventHandler.getRectangleClick(buttons.get(button).x, buttons.get(button).y, buttons.get(button).width + buttons.get(button).x, buttons.get(button).height + buttons.get(button).y);

            if (buttons.get(button).name.equals(json.getName("Play")) && buttons.get(button).isClicked && !Window.start) {
                Video.video.get(defPath + "\\src\\assets\\World\\kaif.mp4").isPlaying = false;
                MainMenu.delete();
                Window.start = true;
                new Thread(new Physics()).start();
                new Thread(new CreaturesGenerate()).start();
            }
            if (buttons.get(button).name.equals(json.getName("Exit")) && buttons.get(button).isClicked) {
                logger.logExit(0);
            }
        }
    }

    public static void updateKeyShortcut() {
        if ((getKey(GLFW_KEY_LEFT_ALT) || getKey(GLFW_KEY_RIGHT_ALT)) && getKey(GLFW_KEY_F4)) {
            logger.logExit(0);
        }
    }
}
