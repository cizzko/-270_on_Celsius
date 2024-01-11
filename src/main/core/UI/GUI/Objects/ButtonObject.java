package core.UI.GUI.Objects;

import core.UI.GUI.CreateElement;
import core.Utils.SimpleColor;
import core.g2d.Atlas;

public class ButtonObject extends Element {
    public boolean visible, isClicked, simple, swapButton, isClickable;
    public long lastClickTime;
    public SimpleColor color;
    public String name, prompt, group;
    public Atlas.Region texture;
    public Runnable taskOnClick;

    public ButtonObject(boolean simple, boolean swapButton, int x, int y, int height, int width, String name, String prompt, SimpleColor color, String group, Runnable taskOnClick) {
        super(x, y, width, height);
        if (group == null) {
            group = "None";
        }

        this.isClicked = false;
        this.isClickable = true;
        this.visible = true;
        this.simple = simple;
        this.swapButton = swapButton;
        this.texture = null;
        this.name = name;
        this.group = group;
        this.prompt = prompt;
        this.color = color;
        this.lastClickTime = System.currentTimeMillis();
        this.taskOnClick = taskOnClick;
    }

    public static Runnable onClickDropButton(String buttonName, String[] names) {
        return () -> {
            if (System.currentTimeMillis() - CreateElement.buttons.get(buttonName).lastClickTime >= 150) {
                for (String s : names) {
                    CreateElement.buttons.get(s).visible = !CreateElement.buttons.get(s).visible;
                }
                CreateElement.buttons.get(buttonName).lastClickTime = System.currentTimeMillis();
            }
        };
    }
}
