package core.UI.hud;

import core.EventHandling.EventHandler;
import core.UI.Dialog;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;

import static core.Global.atlas;
import static core.Global.input;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;

public final class HeadUpDisplay extends Dialog {
    public HeadUpDisplay() {
        setTouchable(false);
        setMaximized(true);

        addImageButton(WorkbenchLogic::toggleBuildMenu)
                .setPosition(1650, 0)
                .setImage(atlas.get("UI/GUI/buildMenu/menuClosed"))
                .setHotkey(GLFW_KEY_B, WorkbenchLogic::toggleBuildMenu);


    }
}
