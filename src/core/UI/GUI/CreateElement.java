package core.UI.GUI;

import core.UI.GUI.Objects.ButtonObject;
import core.UI.GUI.Objects.PanelObject;
import core.UI.GUI.Objects.SliderObject;
import core.UI.GUI.Objects.TextObject;
import core.World.Textures.SimpleColor;
import core.World.Textures.TextureLoader;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import static core.World.Textures.TextureLoader.getSize;

public class CreateElement {
    public static ConcurrentHashMap<String, ButtonObject> buttons = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, SliderObject> sliders = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, PanelObject> panels = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ButtonObject[]> dropMenu = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, TextObject> texts = new ConcurrentHashMap<>();

    public static void createButton(int x, int y, int btnWidth, int btnHeight, String name, String prompt, boolean simple, SimpleColor color, String group, Runnable taskOnClick) {
        buttons.put(name, new ButtonObject(simple, false, x, y, btnHeight, btnWidth, name, prompt, color, group, taskOnClick));
    }

    public static void createDropMenu(int x, int y, int menuWidth, int menuHeight, String[] btnNames, String pressedButton, String menuName, SimpleColor color, String group) {
        ButtonObject[] dropButtons = new ButtonObject[btnNames.length];
        buttons.put(menuName, new ButtonObject(true, false, x, y, menuHeight, menuWidth, menuName, null, color, group, null));

        for (int i = 0; i < btnNames.length; i++) {
            if (i == 0) {
                dropButtons[i] = new ButtonObject(true, true, x, y - menuHeight, menuHeight, menuWidth, btnNames[i], null, color, group, null);
            } else {
                dropButtons[i] = new ButtonObject(true, true, x, dropButtons[i - 1].y - menuHeight, menuHeight, menuWidth, btnNames[i], null, color, group, null);
            }
            dropButtons[i].visible = false;

            if (dropButtons[i].name.equals(pressedButton)) {
                dropButtons[i].isClicked = true;
            }
        }
        dropMenu.put(menuName, dropButtons);
    }

    public static void createPictureButton(int x, int y, String path, String name, String group, Runnable taskOnClick) {
        buttons.put(name, new ButtonObject(true, false, x, y, TextureLoader.getSize(path).height, TextureLoader.getSize(path).width, name, null, new SimpleColor(255, 255, 255, 255), group, taskOnClick));
        buttons.get(name).path = path;
    }

    public static void createSwapButton(int x, int y, int btnWidth, int btnHeight, String name, String prompt, boolean simple, SimpleColor color, boolean isClicked, String group) {
        if (simple) {
            buttons.put(name, new ButtonObject(simple, true, x, y, btnHeight, btnWidth, name, prompt, color, group, null));
        } else {
            buttons.put(name, new ButtonObject(simple, true, x, y, 44, 44, name, prompt, color, group, null));
        }
        if (isClicked) {
            buttons.get(name).isClicked = true;
        }
    }

    public static void createSwapButton(int x, int y, int btnWidth, int btnHeight, String name, String prompt, boolean simple, SimpleColor color, String group) {
        createSwapButton(x, y, btnWidth, btnHeight, name, prompt, simple, color, false, group);
    }

    public static void createSlider(int x, int y, int sliderWidth, int sliderHeight, int max, String name, SimpleColor sliderColor, SimpleColor dotColor) {
        sliders.put(name, new SliderObject(x, y, sliderWidth, sliderHeight, max, sliderColor, dotColor));
        sliders.get(name).sliderPos = x + 1;
    }

    public static int getSliderPos(String name) {
        SliderObject slider = sliders.get(name);
        float relativePos = (float) (slider.sliderPos - slider.x) / slider.width;
        return Math.round(relativePos * slider.max);
    }

    public static void createPanel(int x, int y, int panWidth, int panHeight, String name, boolean simple, String group) {
        panels.put(name, new PanelObject(x, y, panWidth, panHeight, 1, name, simple, null, group, new SimpleColor(40, 40, 40, 240)));
    }

    public static void createText(int x, int y, String name, String text, SimpleColor color, String group) {
        texts.put(name, new TextObject(x, y, text, color, group));
    }

    public static void createPicture(int x, int y, int layer, String name, String path, String group) {
        panels.put(name, new PanelObject(x, y, getSize(path).width, getSize(path).height, layer, name, true, path, group, new SimpleColor(255, 255, 255, 255)));
    }
}
