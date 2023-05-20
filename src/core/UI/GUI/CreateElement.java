package core.UI.GUI;

import core.EventHandling.EventHandler;
import core.UI.GUI.objects.ButtonObject;
import core.UI.GUI.objects.PanelObject;
import core.UI.GUI.objects.SliderObject;
import core.EventHandling.Logging.json;
import core.EventHandling.Logging.logger;
import core.UI.GUI.Menu.CreatePlanetMenu;
import core.UI.GUI.Menu.MainMenu;
import core.Window;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import core.World.creatures.CreaturesGenerate;
import core.World.creatures.Physics;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import static core.EventHandling.EventHandler.getKey;
import static core.Window.defPath;
import static core.Window.glfwWindow;
import static core.World.Textures.TextureLoader.BufferedImageEncoder;
import static org.lwjgl.glfw.GLFW.*;

public class CreateElement extends Thread {
    public static ConcurrentHashMap<String, ButtonObject> buttons = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, SliderObject> sliders = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, PanelObject> panels = new ConcurrentHashMap<>();

    public static void createButton(int x, int y, int width, int height, String name, boolean simple, Color color) {
        buttons.put(name, new ButtonObject(simple, false, true, x, y, height, width, name, color));
    }

    public static void createSwapButton(int x, int y, int width, int height, String name, boolean simple, Color color) {
        if (simple) {
            buttons.put(name, new ButtonObject(simple, true, true, x, y, height, width, name, color));
        } else {
            buttons.put(name, new ButtonObject(simple, true, true, x, y, 44, 44, name, color));
        }
    }

    public static void createSlider(int x, int y, int width, int height, int max, String name, Color sliderColor, Color dotColor) {
        sliders.put(name, new SliderObject(true, x, y, width, height, max, sliderColor, dotColor));
        sliders.get(name).sliderPos = x + 1;
    }

    public static int getSliderPos(String name) {
        SliderObject slider = sliders.get(name);
        float relativePos = (float) (slider.sliderPos - slider.x) / slider.width;
        return Math.round(relativePos * slider.max);
    }

    public static void createPanel(int x, int y, int width, int height, String name, boolean simple) {
        panels.put(name, new PanelObject(x, y, width, height, 1, name, true, simple, null));
    }

    public static void createPicture(int x, int y, int layer, String name, String path) {
        panels.put(name, new PanelObject(x, y, BufferedImageEncoder(path).getWidth(), BufferedImageEncoder(path).getHeight(), layer, name, true, true, path));
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

    public static void updateButtons() {
        for (ButtonObject button : buttons.values()) {
            if (!button.visible) {
                button.isClicked = false;
                continue;
            }

            if (button.swapButton) {
                if (System.currentTimeMillis() - button.lastClickTime >= 150 && EventHandler.getRectanglePress(button.x, button.y, button.width + button.x, button.height + button.y)) {
                    button.isClicked = !button.isClicked;
                    button.lastClickTime = System.currentTimeMillis();
                }
            } else {
                button.isClicked = EventHandler.getRectanglePress(button.x, button.y, button.width + button.x, button.height + button.y);
            }

            if (button.name.equals(json.getName("Play")) && button.isClicked && !Window.start) {
                MainMenu.delete();
                CreatePlanetMenu.create();
            }
            if (button.name.equals(json.getName("GenerateWorld")) && button.isClicked && !Window.start) {
                WorldGenerator.generateWorld(getSliderPos("worldSize"), 20, false);
                WorldGenerator.generateDynamicsObjects();
                TextureDrawing.loadObjects();

                Video.video.get(defPath + "\\src\\assets\\World\\kaif.mp4").isPlaying = false;
                CreatePlanetMenu.delete();

                new Thread(new Physics()).start();
                new Thread(new CreaturesGenerate()).start();
                Window.start = true;
            }
            if (button.name.equals(json.getName("Exit")) && button.isClicked) {
                logger.logExit(0);
            }

            //дико накостылил
            if (sliders.get("worldSize") != null && getSliderPos("worldSize") <= sliders.get("worldSize").max / 3) {
                createPicture(1510, 670, 2, "planet", defPath + "\\src\\assets\\World\\planetMini.png");
            }
            if (sliders.get("worldSize") != null && getSliderPos("worldSize") >= sliders.get("worldSize").max / 3) {
                createPicture(1510, 670, 2, "planet", defPath + "\\src\\assets\\World\\planetAverage.png");
            }
            if (sliders.get("worldSize") != null && getSliderPos("worldSize") >= sliders.get("worldSize").max / 2) {
                createPicture(1510, 670, 2, "planet", defPath + "\\src\\assets\\World\\planetBig.png");
            }
        }
    }


    public static void updateKeyShortcut() {
        if (((getKey(GLFW_KEY_LEFT_ALT) || getKey(GLFW_KEY_RIGHT_ALT)) && getKey(GLFW_KEY_F4)) || getKey(GLFW_KEY_F7)) {
            logger.logExit(0);
        }
    }
}
