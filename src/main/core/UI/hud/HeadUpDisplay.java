package core.UI.hud;

import core.UI.Dialog;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;

import static core.Global.atlas;

public final class HeadUpDisplay extends Dialog {
    public HeadUpDisplay() {
        setTouchable(false);
        setMaximized(true);

        addImageButton(this::onBuildMenu)
                .setPosition(1650, 0)
                .setImage(atlas.get("UI/GUI/buildMenu/menuClosed"));
    }

    private void onBuildMenu() {
        WorkbenchLogic.toggleBuildMenu();
    }
}
