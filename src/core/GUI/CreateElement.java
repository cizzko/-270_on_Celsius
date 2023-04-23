package core.GUI;

import core.EventHandling.EventHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static core.Window.glfwWindow;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class CreateElement extends Thread {
    public static ConcurrentHashMap<String, GuiObjects> elements = new ConcurrentHashMap<>();

    public static void createButton(int x, int y, String path, String name, Boolean visible) {
        GuiObjects button = new GuiObjects(false, visible, x, y, name, path);
        button.isButton = true;
    }

    public static void createPanel(int x, int y, int min, int max, String name, Boolean visible) {
        GuiObjects button = new GuiObjects(false, visible, x, y, name, null);
        button.isPanel = true;
    }

    public static void createSlider(int x, int y, String name, Boolean visible) {
        GuiObjects button = new GuiObjects(false, visible, x, y, name, null);
        button.isSlider = true;
    }

    public static void setVisible(String name, boolean visible) {
        elements.get(name).visible = visible;
    }

    @Override
    public void run() {
        while (!glfwWindowShouldClose(glfwWindow)) {
            for (Map.Entry<String, GuiObjects> entry : elements.entrySet()) {
                String button = entry.getKey();

                if (elements.get(button).isButton && EventHandler.getRectangleClick(elements.get(button).x, elements.get(button).y, elements.get(button).path) && elements.get(button).visible) {
                    elements.get(button).isClicked = true;
                } else if (elements.get(button).isButton && !EventHandler.getRectangleClick(elements.get(button).x, elements.get(button).y, elements.get(button).path) && elements.get(button).visible) {
                    elements.get(button).isClicked = false;
                }
            }
        }
    }
}
