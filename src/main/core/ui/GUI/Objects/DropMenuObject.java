package core.ui.GUI.Objects;

import core.Utils.SimpleColor;
import java.util.Arrays;

public class DropMenuObject extends Element {
    public SimpleColor color;
    public String menuName;
    public String[] btnNames;
    public boolean[] btnIsClicked;
    public boolean isVisible, menuIsClicked;

    public DropMenuObject(int x, int y, int width, int height, String[] btnNames, String menuName, SimpleColor color) {
        super(x, y, width, height);
        this.btnNames = btnNames;
        this.menuName = menuName;
        this.color = color;
        this.btnIsClicked = new boolean[btnNames.length];
        Arrays.fill(btnIsClicked, false);
        this.isVisible = true;
        this.menuIsClicked = true;
    }
}
