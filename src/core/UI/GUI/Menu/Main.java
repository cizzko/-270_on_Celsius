package core.UI.GUI.Menu;

import java.awt.*;
import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;

public class Main {
    public static void create() {
        //default coordinate system - full hd
        createPanel(0, 965, 1920, 115, "defPan", true, "MainMenu");

        createButton(822, 990, 240, 65, getName("Exit"), null, false, new Color(236, 236, 236, 55), "MainMenu");
        createButton(548, 990, 240, 65, getName("Settings"), null, false, new Color(236, 236, 236, 55), "MainMenu");
        createButton(46, 990, 240, 65, getName("Play"), null, false, new Color(255, 80, 0, 55), "MainMenu");
    }

    public static void delete() {
        buttons.values().stream().filter(button -> button.group.equals("MainMenu")).forEach(button -> button.visible = false);

        panels.get("defPan").visible = false;
    }
}
