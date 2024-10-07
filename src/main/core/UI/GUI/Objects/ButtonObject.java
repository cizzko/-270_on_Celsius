package core.UI.GUI.Objects;

import core.UI.GUI.CreateElement;
import core.Utils.SimpleColor;
import core.g2d.Atlas;

public class ButtonObject extends Element {
    private boolean visible, isClicked, simple, swapButton, isClickable;
    private SimpleColor color;
    private String name, prompt, group;
    private Atlas.Region texture;
    private Runnable taskOnClick;

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
        this.taskOnClick = taskOnClick;
    }

    public static Runnable onClickDropButton(String[] names) {
        return () -> {
            for (String s : names) {
                CreateElement.buttons.get(s).visible = !CreateElement.buttons.get(s).visible;
            }
        };
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isClicked() {
        return isClicked;
    }

    public void setClicked(boolean clicked) {
        isClicked = clicked;
    }

    public boolean isSimple() {
        return simple;
    }

    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    public boolean isSwapButton() {
        return swapButton;
    }

    public void setSwapButton(boolean swapButton) {
        this.swapButton = swapButton;
    }

    public boolean isClickable() {
        return isClickable;
    }

    public void setClickable(boolean clickable) {
        isClickable = clickable;
    }

    public SimpleColor getColor() {
        return color;
    }

    public void setColor(SimpleColor color) {
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Atlas.Region getTexture() {
        return texture;
    }

    public void setTexture(Atlas.Region texture) {
        this.texture = texture;
    }

    public Runnable getTaskOnClick() {
        return taskOnClick;
    }

    public void setTaskOnClick(Runnable taskOnClick) {
        this.taskOnClick = taskOnClick;
    }
}
