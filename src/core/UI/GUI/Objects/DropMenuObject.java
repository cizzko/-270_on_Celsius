package core.UI.GUI.Objects;

import core.Utils.SimpleColor;
import java.util.Arrays;

public class DropMenuObject {
    public int x, y, width, height;
    public SimpleColor color;
    public String menuName;
    public String[] btnNames;
    public boolean[] btnIsClicked;
    public boolean isVisible, menuIsClicked;

    public DropMenuObject(int x, int y, int width, int height, String[] btnNames, String menuName, SimpleColor color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.btnNames = btnNames;
        this.menuName = menuName;
        this.color = color;
        this.btnIsClicked = new boolean[btnNames.length];
        Arrays.fill(btnIsClicked, false);
        this.isVisible = true;
        this.menuIsClicked = true;
    }
}
