package core.UI.GUI;

import core.UI.GUI.objects.ButtonObject;
import core.UI.GUI.objects.PanelObject;
import core.UI.GUI.objects.SliderObject;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import static core.World.Textures.TextureLoader.BufferedImageEncoder;

public class CreateElement {
    public static ConcurrentHashMap<String, ButtonObject> buttons = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, SliderObject> sliders = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, PanelObject> panels = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ButtonObject[]> dropMenu = new ConcurrentHashMap<>();

    public static void createButton(int x, int y, int width, int height, String name, boolean simple, Color color) {
        buttons.put(name, new ButtonObject(simple, false, x, y, height, width, name, color));
    }

    public static void createDropMenu(int x, int y, int width, int height, String[] btnNames, String menuName, Color color) {
        ButtonObject[] dropButtons = new ButtonObject[btnNames.length];
        buttons.put(menuName, new ButtonObject(true, false, x, y, height, width, menuName, color));

        for (int i = 0; i < btnNames.length; i++) {
            if (i == 0) {
                dropButtons[i] = new ButtonObject(true, true, x, y - height, height, width, btnNames[i], color);
            } else {
                dropButtons[i] = new ButtonObject(true, true, x, dropButtons[i - 1].y - height, height, width, btnNames[i], color);
            }
            dropButtons[i].visible = false;
        }
        dropMenu.put(menuName, dropButtons);
    }

    public static void createSwapButton(int x, int y, int width, int height, String name, boolean simple, Color color) {
        if (simple) {
            buttons.put(name, new ButtonObject(simple, true, x, y, height, width, name, color));
        } else {
            buttons.put(name, new ButtonObject(simple, true, x, y, 44, 44, name, color));
        }
    }

    public static void createSlider(int x, int y, int width, int height, int max, String name, Color sliderColor, Color dotColor) {
        sliders.put(name, new SliderObject(x, y, width, height, max, sliderColor, dotColor));
        sliders.get(name).sliderPos = x + 1;
    }

    public static int getSliderPos(String name) {
        SliderObject slider = sliders.get(name);
        float relativePos = (float) (slider.sliderPos - slider.x) / slider.width;
        return Math.round(relativePos * slider.max);
    }

    public static void createPanel(int x, int y, int width, int height, String name, boolean simple) {
        panels.put(name, new PanelObject(x, y, width, height, 1, name, simple, null));
    }

    public static void createPicture(int x, int y, int layer, String name, String path) {
        panels.put(name, new PanelObject(x, y, BufferedImageEncoder(path).getWidth(), BufferedImageEncoder(path).getHeight(), layer, name, true, path));
    }
}
