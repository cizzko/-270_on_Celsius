package core.UI.GUI.Menu;

import core.EventHandling.Logging.json;
import java.awt.*;
import static core.UI.GUI.CreateElement.*;
import static core.Window.defPath;

public class CreatePlanetMenu {
    public static void create() {
        createPanel(20, 20, 1880, 200, "downPanel", false);
        createPanel(20, 240, 1400, 820, "leftPanel", false);
        createPanel(1440, 240, 460, 820, "rightPanel", false);

        createPicture(1460, 620, 1, "planetBackground", defPath + "\\src\\assets\\World\\neboLol.png");
        createPicture(1510, 670, 2, "planet", defPath + "\\src\\assets\\World\\planetMini.png");

        createButton(1460, 260, 420, 67, json.getName("GenerateWorld"), true, new Color(255, 80, 0, 55));
        createSwapButton(70, 980, 32, 32, json.getName("GenerateSimpleWorld"), false, new Color(236, 236, 236, 55));

        createSlider(1460, 340, 420, 20, 2500, "worldSize", new Color(40, 40, 40, 240), new Color(255, 80, 0, 119));
    }

    public static void delete() {
        panels.get("downPanel").visible = false;
        panels.get("leftPanel").visible = false;
        panels.get("rightPanel").visible = false;
        panels.get("planet").visible = false;
        panels.get("planetBackground").visible = false;

        buttons.get(json.getName("GenerateWorld")).visible = false;
        buttons.get(json.getName("GenerateSimpleWorld")).visible = false;
        buttons.get("Generating float world..").visible = false;

        sliders.get("worldSize").visible = false;
    }
}
