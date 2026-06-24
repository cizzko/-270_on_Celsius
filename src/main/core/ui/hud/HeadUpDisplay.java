package core.ui.hud;

import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;
import core.ui.Align;
import core.ui.ClickType;
import core.ui.Table;
import org.lwjgl.glfw.GLFW;

import static core.ui.widget.Widgets.atlasImage;

public final class HeadUpDisplay extends Table {
    public HeadUpDisplay() {
        super("HeadUpDisplay");
        setTouchable(false);
        setFillParent(true);

        cell(atlasImage("UI/GUI/buildMenu/menuClosed"), c -> {
            c.align(Align.RIGHT);
            c.padRight(200);
            c.expandX();

            c.widget.setClickKey(GLFW.GLFW_MOUSE_BUTTON_1, ClickType.PRESS, () -> {
                WorkbenchLogic.toggleBuildMenu();
            });
        });
    }
}
