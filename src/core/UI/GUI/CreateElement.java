package core.UI.GUI;

import core.UI.GUI.Objects.ButtonObject;
import core.UI.GUI.Objects.PanelObject;
import core.UI.GUI.Objects.SliderObject;
import core.UI.GUI.Objects.TextObject;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import static core.Window.height;
import static core.Window.width;
import static core.World.Textures.TextureLoader.BufferedImageEncoder;

public class CreateElement {
    public static ConcurrentHashMap<String, ButtonObject> buttons = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, SliderObject> sliders = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, PanelObject> panels = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ButtonObject[]> dropMenu = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, TextObject> texts = new ConcurrentHashMap<>();

    public static void createButton(int x, int y, int btnWidth, int btnHeight, String name, String prompt, boolean simple, Color color) {
        int newX = (int) Math.round((double) x / 1920 * width);
        int newY = (int) Math.round((double) y / 1080 * height);
        int newWidth = (int) Math.round((double) btnWidth / 1920 * width);
        int newHeight = (int) Math.round((double) btnHeight / 1080 * height);

        buttons.put(name, new ButtonObject(simple, false, newX, newY, newHeight, newWidth, name, prompt, color));
    }

    public static void createDropMenu(int x, int y, int menuWidth, int menuHeight, String[] btnNames, String menuName, Color color) {
        int newX = (int) Math.round((double) x / 1920 * width);
        int newY = (int) Math.round((double) y / 1080 * height);
        int newWidth = (int) Math.round((double) menuWidth / 1920 * width);
        int newHeight = (int) Math.round((double) menuHeight / 1080 * height);

        ButtonObject[] dropButtons = new ButtonObject[btnNames.length];
        buttons.put(menuName, new ButtonObject(true, false, newX, newY, newHeight, newWidth, menuName, null, color));

        for (int i = 0; i < btnNames.length; i++) {
            if (i == 0) {
                dropButtons[i] = new ButtonObject(true, true, newX, newY - newHeight, newHeight, newWidth, btnNames[i], null, color);
            } else {
                dropButtons[i] = new ButtonObject(true, true, newX, dropButtons[i - 1].y - newHeight, newHeight, newWidth, btnNames[i], null, color);
            }
            dropButtons[i].visible = false;
        }
        dropMenu.put(menuName, dropButtons);
    }

    public static void createSwapButton(int x, int y, int btnWidth, int btnHeight, String name, String prompt, boolean simple, Color color) {
        int newX = (int) Math.round((double) x / 1920 * width);
        int newY = (int) Math.round((double) y / 1080 * height);
        int newWidth = (int) Math.round((double) btnWidth / 1920 * width);
        int newHeight = (int) Math.round((double) btnHeight / 1080 * height);

        if (simple) {
            buttons.put(name, new ButtonObject(simple, true, newX, newY, newHeight, newWidth, name, prompt, color));
        } else {
            buttons.put(name, new ButtonObject(simple, true, newX, newY, 44, 44, name, prompt, color));
        }
    }

    public static void createSlider(int x, int y, int sliderWidth, int sliderHeight, int max, String name, Color sliderColor, Color dotColor) {
        int newX = (int) Math.round((double) x / 1920 * width);
        int newY = (int) Math.round((double) y / 1080 * height);
        int newWidth = (int) Math.round((double) sliderWidth / 1920 * width);
        int newHeight = (int) Math.round((double) sliderHeight / 1080 * height);

        sliders.put(name, new SliderObject(newX, newY, newWidth, newHeight, max, sliderColor, dotColor));
        sliders.get(name).sliderPos = newX + 1;
    }

    public static int getSliderPos(String name) {
        SliderObject slider = sliders.get(name);
        float relativePos = (float) (slider.sliderPos - slider.x) / slider.width;
        return Math.round(relativePos * slider.max);
    }

    public static void createPanel(int x, int y, int panWidth, int panHeight, String name, boolean simple) {
        int newX = (int) Math.round((double) x / 1920 * width);
        int newY = (int) Math.round((double) y / 1080 * height);
        int newWidth = (int) Math.round((double) panWidth / 1920 * width);
        int newHeight = (int) Math.round((double) panHeight / 1080 * height);

        panels.put(name, new PanelObject(newX, newY, newWidth, newHeight, 1, name, simple, null));
    }

    public static void createText(int x, int y, String name, String text, Color color) {
        texts.put(name, new TextObject(x, y, text, color));
    }

    public static void createPicture(int x, int y, int layer, String name, String path) {
        int newX = (int) Math.round((double) x / 1920 * width);
        int newY = (int) Math.round((double) y / 1080 * height);
        int newWidth = (int) Math.round((double) BufferedImageEncoder(path).getWidth() / 1920 * width);
        int newHeight = (int) Math.round((double) BufferedImageEncoder(path).getHeight() / 1080 * height);

        panels.put(name, new PanelObject(newX, newY, newWidth, newHeight, layer, name, true, path));
    }
}
