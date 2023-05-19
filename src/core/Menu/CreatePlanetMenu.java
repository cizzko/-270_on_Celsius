package core.Menu;

import core.Logging.json;
import core.Window;
import java.awt.*;
import static core.GUI.CreateElement.*;
import static core.Window.defPath;

public class CreatePlanetMenu {
    public static void create() {
        int width = Window.width;   //ширина
        int height = Window.height; //высота

        createPanel(20, 20, width - 40, 200, "downPanel", false);
        createPanel(20, 240, 1400, height - 260, "leftPanel", false);
        createPanel(1440, 240, width - 1460, height - 260, "rightPanel", false);

        createPicture(1460, 620, 1, "planetBackground", defPath + "\\src\\assets\\World\\neboLol.png");
        createPicture(1510, 670, 2, "planet", defPath + "\\src\\assets\\World\\planetAverage.png");

        createButton(1460, 260, width - 1500, 67, json.getName("GenerateWorld"), true, false, new Color(255, 80, 0, 55));
    }

    public static void delete() {
        panels.get("downPanel").visible = false;
        panels.get("leftPanel").visible = false;
        panels.get("rightPanel").visible = false;
        panels.get("planet").visible = false;
        panels.get("planetBackground").visible = false;
        buttons.get(json.getName("GenerateWorld")).visible = false;
    }
}
