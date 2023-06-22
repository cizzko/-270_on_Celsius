package core.UI.GUI.Menu;

import java.awt.*;

import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;

public class Pause {
    public static boolean created = false;

    public static void create() {
        if (!created) {
            createPanel(0, 0, 1920, 1080, "Panel", true, "Pause");

            createButton(840, 600, 240, 65, getName("Exit"), null, false, new Color(236, 236, 236, 55), "Pause");
            createButton(840, 500, 240, 65, getName("Settings"), null, false, new Color(236, 236, 236, 55), "Pause");
            createButton(840, 400, 240, 65, getName("Continue"), null, false, new Color(255, 80, 0, 55), "Pause");
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
}
