package core.GUI;

import core.EventHandling.EventHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static core.EventHandling.EventHandler.getKey;
import static core.Window.glfwWindow;
import static org.lwjgl.glfw.GLFW.*;

public class CreateElement extends Thread {
    public static ConcurrentHashMap<String, ButtonObject> buttons = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, SliderObject> sliders = new ConcurrentHashMap<>();

    public static void createButton(int x, int y, int width, int height, String name, Boolean visible) {
        buttons.put(name, new ButtonObject(visible, x, y, height, width, name));
    }

    public static void createSlider(int x, int y, int width, int height, int max, String name, Boolean visible) {
        sliders.put(name, new SliderObject(visible, x, y, width, height, max));
        sliders.get(name).sliderPos = x + 1;
    }

    @Override
    public void run() {
        while (!glfwWindowShouldClose(glfwWindow)) {
            for (Map.Entry<String, ButtonObject> entry : buttons.entrySet()) {
                String button = entry.getKey();

                if (EventHandler.getRectangleClick(buttons.get(button).x, buttons.get(button).y, buttons.get(button).width, buttons.get(button).height) && buttons.get(button).visible) {
                    buttons.get(button).isClicked = true;
                } else if (!EventHandler.getRectangleClick(buttons.get(button).x, buttons.get(button).y, buttons.get(button).width, buttons.get(button).height) && buttons.get(button).visible) {
                    buttons.get(button).isClicked = false;
                }
                if (!buttons.get(button).visible) {
                    buttons.get(button).isClicked = false;
                }
            }
            for (Map.Entry<String, SliderObject> entry : sliders.entrySet()) {
                String slider = entry.getKey();

                if (!sliders.get(slider).visible) {
                    continue;
                }
                if (EventHandler.getRectangleClick(sliders.get(slider).x, sliders.get(slider).y, sliders.get(slider).width,sliders.get(slider).height + sliders.get(slider).y)) {
                    sliders.get(slider).sliderPos = EventHandler.getMousePos().x;
                }
            }
        }
    }
}
