package core.UI.GUI.Menu;

import core.World.Textures.SimpleColor;
import core.World.WorldGenerator;

import java.awt.*;
import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;
import static core.Window.defPath;

public class CreatePlanet {

    public static void create() {
        createPanel(20, 20, 1880, 200, "downPanel", false, "WorldGenerator");
        createPanel(20, 240, 1400, 820, "leftPanel", false, "WorldGenerator");
        createPanel(1440, 240, 460, 820, "rightPanel", false, "WorldGenerator");

        createPicture(1460, 620, 1, "planetBackground", defPath + "\\src\\assets\\World\\worldGenerator\\skyBackgroundPlanet.png", "WorldGenerator");
        createPicture(1510, 670, 2, "planet", defPath + "\\src\\assets\\World\\worldGenerator\\planetMini.png", "WorldGenerator");

        createButton(1460, 260, 420, 67, getName("GenerateWorld"), null, true, new SimpleColor(255, 80, 0, 55), "WorldGenerator", WorldGenerator::generateWorld);
        createSwapButton(70, 980, 32, 32, getName("GenerateSimpleWorld"), getName("GenerateSimpleWorldPrompt"), false, new SimpleColor(236, 236, 236, 55), "WorldGenerator");
        createSwapButton(70, 910, 32, 32, getName("GenerateCreatures"), getName("GenerateCreaturesPrompt"), false, new SimpleColor(236, 236, 236, 55), true, "WorldGenerator");
        createSwapButton(70, 840, 32, 32, getName("RandomSpawn"), getName("RandomSpawnPrompt"), false, new SimpleColor(236, 236, 236, 55), "WorldGenerator");

        createSlider(1460, 340, 420, 20, 2500, "worldSize", new SimpleColor(40, 40, 40, 240), new SimpleColor(255, 80, 0, 119));
    }

    public static void delete() {
        panels.values().stream().filter(button -> button.group.equals("WorldGenerator")).forEach(button -> button.visible = false);
        buttons.values().stream().filter(button -> button.group.equals("WorldGenerator")).forEach(button -> button.visible = false);
        texts.values().stream().filter(button -> button.group.equals("WorldGeneratorState")).forEach(button -> button.visible = false);

        sliders.get("worldSize").visible = false;
    }
}
