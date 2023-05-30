package core.UI.GUI.Menu;

import java.awt.*;
import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;

public class Main {
    public static void create() {
        //default coordinate system - full hd
        createPanel(0, 965, 1920, 115, "defPan", true);

        createButton(822, 990, 240, 65, getName("Exit"), false, new Color(236, 236, 236, 55));
        createButton(548, 990, 240, 65, getName("Settings"), false, new Color(236, 236, 236, 55));
        createButton(46, 990, 240, 65, getName("Play"), false, new Color(255, 80, 0, 55));
    }

    public static void delete() {
        buttons.get(getName("Exit")).visible = false;
        buttons.get(getName("Settings")).visible = false;
        buttons.get(getName("Play")).visible = false;

        panels.get("defPan").visible = false;
    }
}
