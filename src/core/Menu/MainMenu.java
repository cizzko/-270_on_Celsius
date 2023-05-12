package core.Menu;

import core.Logging.json;
import core.Window;
import java.awt.*;
import static core.GUI.CreateElement.*;

public class MainMenu {
    public static void create() {
        int width = Window.width;
        int height = Window.height;

        createPanel(width - (width / 8) * 2, 0, width / 8,height, "defPan", true, true);

        createButton(width - (width / 8) * 2, (int) (height / 13.9f), width / 8, height / 13, json.getName("Exit"), true, new Color(0, 0, 0, 55));
        createButton(width - (width / 8) * 2, (int) (height / 4.19f), width / 8, height / 13, json.getName("Settings"), true, new Color(0, 0, 0, 55));
        createButton(width - (width / 8) * 2, (int) (height / 1.19f), width / 8, height / 13, json.getName("Play"), true, new Color(234, 80, 0, 55));
    }

    public static void delete() {
        buttons.get(json.getName("Exit")).visible = false;
        buttons.get(json.getName("Settings")).visible = false;
        buttons.get(json.getName("Play")).visible = false;

        panels.get("defPan").visible = false;
    }
}
