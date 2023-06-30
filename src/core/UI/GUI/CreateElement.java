package core.UI.GUI;

import core.UI.GUI.Objects.ButtonObject;
import core.UI.GUI.Objects.PanelObject;
import core.UI.GUI.Objects.SliderObject;
import core.UI.GUI.Objects.TextObject;
import core.World.Textures.TextureLoader;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import static core.World.Textures.TextureLoader.BufferedImageEncoder;

public class CreateElement {
    public static ConcurrentHashMap<String, ButtonObject> buttons = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, SliderObject> sliders = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, PanelObject> panels = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ButtonObject[]> dropMenu = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, TextObject> texts = new ConcurrentHashMap<>();

    public static void createButton(int x, int y, int btnWidth, int btnHeight, String name, String prompt, boolean simple, Color color, String group) {
        buttons.put(name, new ButtonObject(simple, false, x, y, btnHeight, btnWidth, name, prompt, color, group));
    }

    public static void createDropMenu(int x, int y, int menuWidth, int menuHeight, String[] btnNames, String menuName, Color color, String group) {
        ButtonObject[] dropButtons = new ButtonObject[btnNames.length];
        buttons.put(menuName, new ButtonObject(true, false, x, y, menuHeight, menuWidth, menuName, null, color, group));

        for (int i = 0; i < btnNames.length; i++) {
            if (i == 0) {
                dropButtons[i] = new ButtonObject(true, true, x, y - menuHeight, menuHeight, menuWidth, btnNames[i], null, color, group);
            } else {
                dropButtons[i] = new ButtonObject(true, true, x, dropButtons[i - 1].y - menuHeight, menuHeight, menuWidth, btnNames[i], null, color, group);
            }
            dropButtons[i].visible = false;
        }
        dropMenu.put(menuName, dropButtons);
    }

    public static void createPictureButton(int x, int y, String path, String name, String group) {
        buttons.put(name, new ButtonObject(true, false, x, y, TextureLoader.BufferedImageEncoder(path).getHeight(), TextureLoader.BufferedImageEncoder(path).getWidth(), name, null, new Color(255, 255, 255, 255), group));
        buttons.get(name).path = path;
    }

    public static void createSwapButton(int x, int y, int btnWidth, int btnHeight, String name, String prompt, boolean simple, Color color, boolean isClicked, String group) {
        if (simple) {
            buttons.put(name, new ButtonObject(simple, true, x, y, btnHeight, btnWidth, name, prompt, color, group));
        } else {
            buttons.put(name, new ButtonObject(simple, true, x, y, 44, 44, name, prompt, color, group));
        }
        if (isClicked) {
            buttons.get(name).isClicked = true;
        }
    }

    public static void createSwapButton(int x, int y, int btnWidth, int btnHeight, String name, String prompt, boolean simple, Color color, String group) {
        createSwapButton(x, y, btnWidth, btnHeight, name, prompt, simple, color, false, group);
    }

    public static void createSlider(int x, int y, int sliderWidth, int sliderHeight, int max, String name, Color sliderColor, Color dotColor) {
        sliders.put(name, new SliderObject(x, y, sliderWidth, sliderHeight, max, sliderColor, dotColor));
        sliders.get(name).sliderPos = x + 1;
    }

    public static int getSliderPos(String name) {
        SliderObject slider = sliders.get(name);
        float relativePos = (float) (slider.sliderPos - slider.x) / slider.width;
        return Math.round(relativePos * slider.max);
    }

    public static void createPanel(int x, int y, int panWidth, int panHeight, String name, boolean simple, String group) {
        panels.put(name, new PanelObject(x, y, panWidth, panHeight, 1, name, simple, null, group, new Color(40, 40, 40, 240)));
    }

    public static void createPanel(int x, int y, int panWidth, int panHeight, String name, String group, Color color) {
        panels.put(name, new PanelObject(x, y, panWidth, panHeight, 1, name, true, null, group, color));
    }

    public static void createText(int x, int y, String name, String text, Color color, String group) {
        texts.put(name, new TextObject(x, y, text, color, group));
    }

    public static void createPicture(int x, int y, int layer, String name, String path, String group) {
        panels.put(name, new PanelObject(x, y, BufferedImageEncoder(path).getWidth(), BufferedImageEncoder(path).getHeight(), layer, name, true, path, group, new Color(255, 255, 255, 255)));
    }
}
