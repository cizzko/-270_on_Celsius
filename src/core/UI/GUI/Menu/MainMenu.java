package core.UI.GUI.Menu;

import core.EventHandling.Logging.json;
import java.awt.*;
import static core.UI.GUI.CreateElement.*;

public class MainMenu {
    public static void create() {
        //default coordinate system - full hd
        createPanel(0, 964, 1920, 116, "defPan", true);

        createButton(822, 990, 240, 67, json.getName("Exit"), false, new Color(236, 236, 236, 55));
        createButton(548, 990, 240, 67, json.getName("Settings"), false, new Color(236, 236, 236, 55));
        createButton(46, 990, 240, 67, json.getName("Play"), false, new Color(255, 80, 0, 55));
    }

    public static void delete() {
        buttons.get(json.getName("Exit")).visible = false;
        buttons.get(json.getName("Settings")).visible = false;
        buttons.get(json.getName("Play")).visible = false;

        panels.get("defPan").visible = false;
    }
}
