package core.UI.GUI.Menu;

import core.EventHandling.Logging.Logger;
import core.Utils.SimpleColor;
import core.World.Creatures.Physics;
import core.World.Saves;

import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;

public class Pause {
    public static boolean created = false;

    public static void create() {
        if (!created) {
            createPanel(0, 0, 1920, 1080, "Panel", true, "Pause");

            createButton(840, 650, 240, 65, getName("Continue"), null, false, SimpleColor.DEFAULT_ORANGE, "Pause", Pause::continueBtn);
            createButton(840, 550, 240, 65, getName("SaveWorld"), null, false, SimpleColor.DEFAULT_ORANGE, "Pause", Pause::saveButton);
            createButton(840, 300, 240, 65, getName("Exit"), null, false, SimpleColor.DIRTY_WHITE, "Pause", Pause::exitBtn);
            createButton(840, 400, 240, 65, getName("Settings"), null, false, SimpleColor.DIRTY_WHITE, "Pause", Pause::settingsBtn);
            created = true;
        }
    }

    public static void delete() {
        if (created) {
            buttons.values().stream().filter(button -> button.group.equals("Pause")).forEach(button -> button.visible = false);
            panels.get("Panel").visible = false;
            created = false;
        }
    }

    private static void continueBtn() {
        Pause.delete();
        Settings.delete();
        Physics.resumePhysics();
    }

    private static void exitBtn() {
        Logger.logExit(0);
    }

    private static void settingsBtn() {
        Settings.create();
        Pause.delete();
    }

    private static void saveButton() {
        Saves.createWorldSave();
    }
}
