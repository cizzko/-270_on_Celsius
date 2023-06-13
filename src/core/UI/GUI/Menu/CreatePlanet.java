package core.UI.GUI.Menu;

import java.awt.*;
import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;
import static core.Window.defPath;

public class CreatePlanet {

    public static void create() {
        createPanel(20, 20, 1880, 200, "downPanel", false);
        createPanel(20, 240, 1400, 820, "leftPanel", false);
        createPanel(1440, 240, 460, 820, "rightPanel", false);

        createPicture(1460, 620, 1, "planetBackground", defPath + "\\src\\assets\\World\\other\\sky\\skyBackgroundPlanet.png");
        createPicture(1510, 670, 2, "planet", defPath + "\\src\\assets\\World\\other\\planetMini.png");

        createButton(1460, 260, 420, 67, getName("GenerateWorld"), null, true, new Color(255, 80, 0, 55));
        createSwapButton(70, 980, 32, 32, getName("GenerateSimpleWorld"), getName("GenerateSimpleWorldPrompt"), false, new Color(236, 236, 236, 55));

        createSlider(1460, 340, 420, 20, 2500, "worldSize", new Color(40, 40, 40, 240), new Color(255, 80, 0, 119));
    }

    public static void delete() {
        panels.get("downPanel").visible = false;
        panels.get("leftPanel").visible = false;
        panels.get("rightPanel").visible = false;
        panels.get("planet").visible = false;
        panels.get("planetBackground").visible = false;

        buttons.get(getName("GenerateWorld")).visible = false;
        buttons.get(getName("GenerateSimpleWorld")).visible = false;

        texts.get("generateFlatWorldText").visible = false;
        texts.get("generatingDone").visible = false;
        if (!buttons.get(getName("GenerateSimpleWorld")).isClicked) {
            texts.get("generateMountainsText").visible = false;
            texts.get("fillHollowsText").visible = false;
            texts.get("smoothWorldText").visible = false;
        }

        sliders.get("worldSize").visible = false;
    }
}
