package core.UI.GUI.Menu;

import core.Utils.SimpleColor;
import core.World.WorldGenerator;

import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;
import static core.Window.assetsDir;

public class CreatePlanet {

    public static void create() {
        createPanel(20, 20, 1880, 200, "downPanel", false, "WorldGenerator");
        createPanel(20, 240, 1400, 820, "leftPanel", false, "WorldGenerator");
        createPanel(1440, 240, 460, 820, "rightPanel", false, "WorldGenerator");

        createPicture(1460, 620, 1, "planetBackground", assetsDir("World/WorldGenerator/skyBackgroundPlanet.png"), "WorldGenerator");
        createPicture(1510, 670, 2, "planet", assetsDir("World/WorldGenerator/planetMini.png"), "WorldGenerator");

        createButton(1460, 260, 420, 67, getName("GenerateWorld"), null, true, SimpleColor.DEFAULT_ORANGE, "WorldGenerator", WorldGenerator::generateWorld);
        createSwapButton(70, 980, 32, 32, getName("GenerateSimpleWorld"), getName("GenerateSimpleWorldPrompt"), false, SimpleColor.DIRTY_WHITE, "WorldGenerator");
        createSwapButton(70, 910, 32, 32, getName("GenerateCreatures"), getName("GenerateCreaturesPrompt"), false, SimpleColor.DIRTY_WHITE, true, "WorldGenerator");
        createSwapButton(70, 840, 32, 32, getName("RandomSpawn"), getName("RandomSpawnPrompt"), false, SimpleColor.DIRTY_WHITE, "WorldGenerator");

        createSlider(1460, 340, 420, 20, 2500, "worldSize", SimpleColor.fromRGBA(40, 40, 40, 240), SimpleColor.DEFAULT_ORANGE);
    }

    public static void delete() {
        panels.values().stream().filter(button -> button.group.equals("WorldGenerator")).forEach(button -> button.visible = false);
        buttons.values().stream().filter(button -> button.group.equals("WorldGenerator")).forEach(button -> button.visible = false);
        texts.values().stream().filter(button -> button.group.equals("WorldGeneratorState")).forEach(button -> button.visible = false);

        sliders.get("worldSize").visible = false;
    }
}
