package core.UI.GUI;

import core.UI.GUI.Objects.ButtonObject;
import core.UI.GUI.Objects.PanelObject;
import core.UI.GUI.Objects.SliderObject;
import core.UI.GUI.Objects.TextObject;
import core.Utils.SimpleColor;
import core.World.Textures.TextureLoader;
import java.util.concurrent.ConcurrentHashMap;
import static core.World.Textures.TextureLoader.getSize;

public class CreateElement {
    public static ConcurrentHashMap<String, ButtonObject> buttons = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, SliderObject> sliders = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, PanelObject> panels = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, TextObject> texts = new ConcurrentHashMap<>();

    public static void createButton(int x, int y, int btnWidth, int btnHeight, String name, String prompt, boolean simple, SimpleColor color, String group, Runnable taskOnClick) {
        buttons.put(name, new ButtonObject(simple, false, x, y, btnHeight, btnWidth, name, prompt, color, group, taskOnClick));
    }

    public static void createDropButton(int x, int y, int btnWidth, int btnHeight, String[] btnNames, String btnName, SimpleColor color, String group, Runnable[] tasks) {
        buttons.put(btnName, new ButtonObject(false, false, x, y, btnHeight, btnWidth, btnName, null, color, group, ButtonObject.onClickDropButton(btnName, btnNames)));

        for (int i = 0; i < btnNames.length; i++) {
            buttons.put(btnNames[i], new ButtonObject(false, false, x, y - (btnHeight * (i + 1)) + (i * 6) + 6, btnHeight, btnWidth, btnNames[i], null, color, group, tasks == null ? null : tasks[i]));
            buttons.get(btnNames[i]).visible = false;
        }
    }

    public static void createPictureButton(int x, int y, String path, String name, String group, Runnable taskOnClick) {
        buttons.put(name, new ButtonObject(true, false, x, y, TextureLoader.getSize(path).height(), TextureLoader.getSize(path).width(), name, null, SimpleColor.WHITE, group, taskOnClick));
        buttons.get(name).path = path;
    }

    public static void createSwapButton(int x, int y, int btnWidth, int btnHeight, String name, String prompt, boolean simple, SimpleColor color, boolean isClicked, String group) {
        buttons.put(name, new ButtonObject(simple, true, x, y, simple ? btnHeight : 44, simple ? btnWidth : 44, name, prompt, color, group, null));
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
        panels.put(name, new PanelObject(x, y, panWidth, panHeight, 1, name, simple, null, group, SimpleColor.fromRGBA(40, 40, 40, 240)));
    }

    public static void createText(int x, int y, String name, String text, SimpleColor color, String group) {
        texts.put(name, new TextObject(x, y, text, color, group));
    }

    public static void createPicture(int x, int y, int layer, String name, String path, String group) {
        panels.put(name, new PanelObject(x, y, getSize(path).width(), getSize(path).height(), layer, name, true, path, group, SimpleColor.WHITE));
    }
}
