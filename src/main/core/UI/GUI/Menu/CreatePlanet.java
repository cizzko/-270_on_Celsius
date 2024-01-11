package core.UI.GUI.Menu;

import core.Global;
import core.Utils.SimpleColor;
import core.World.WorldGenerator;
import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;

public class CreatePlanet {

    public static void create() {
        createPanel(20, 20, 1880, 200, "downPanel", false, "WorldGenerator");
        createPanel(20, 240, 1400, 820, "leftPanel", false, "WorldGenerator");
        createPanel(1440, 240, 460, 820, "rightPanel", false, "WorldGenerator");
        createPanel(40, 955, 1360, 85, "leftAccentPanel", true, "WorldGenerator");

        createPicture(1460, 620, 1, "planetBackground", Global.atlas.byPath("World/WorldGenerator/skyBackgroundPlanet.png"), "WorldGenerator");
        createPicture(1510, 670, 2, "planet", Global.atlas.byPath("World/WorldGenerator/planetMini.png"), "WorldGenerator");

        createButton(40, 975, 240, 65, getName("Return"), null, true, SimpleColor.DEFAULT_ORANGE, "WorldGenerator", CreatePlanet::returnBtn);
        createButton(640, 975, 240, 65, getName("Basic"), null, true, SimpleColor.DEFAULT_ORANGE, "WorldGenerator", CreatePlanet::basicBtn);
        createButton(900, 975, 240, 65, getName("Generation"), null, true, SimpleColor.DEFAULT_ORANGE, "WorldGenerator", CreatePlanet::generationBtn);
        createButton(1160, 975, 240, 65, getName("Physics"), null, true, SimpleColor.DEFAULT_ORANGE, "WorldGenerator", CreatePlanet::physicsBtn);
        createButton(1460, 260, 420, 65, getName("GenerateWorld"), null, true, SimpleColor.DEFAULT_ORANGE, "WorldGenerator", WorldGenerator::generateWorld);

        createSlider(1460, 340, 420, 20, 2500, "worldSize", SimpleColor.fromRGBA(40, 40, 40, 240), SimpleColor.DEFAULT_ORANGE);

        basicBtn();
    }

    public static void delete() {
        panels.values().stream().filter(button -> button.group.equals("WorldGenerator")).forEach(button -> button.visible = false);
        buttons.values().stream().filter(button -> button.group.contains("WorldGenerator")).forEach(button -> button.visible = false);
        texts.values().stream().filter(button -> button.group.equals("WorldGeneratorState")).forEach(button -> button.visible = false);

        sliders.get("worldSize").visible = false;
    }

    private static void returnBtn() {
        delete();
        Main.create();
    }

    private static void basicBtn() {
        deleteGenerationSet();
        deletePhysicsSet();
        createBasicSet();

        buttons.get("Basic").isClickable = false;
        buttons.get("Generation").isClickable = true;
        buttons.get("Physics").isClickable = true;
    }

    private static void generationBtn() {
        deleteBasicSet();
        deletePhysicsSet();
        createGenerationSet();

        buttons.get("Basic").isClickable = true;
        buttons.get("Generation").isClickable = false;
        buttons.get("Physics").isClickable = true;
    }

    private static void physicsBtn() {
        deleteGenerationSet();
        deleteBasicSet();
        createPhysicsSet();

        buttons.get("Basic").isClickable = true;
        buttons.get("Generation").isClickable = true;
        buttons.get("Physics").isClickable = false;
    }

    private static void createBasicSet() {
        createSwapButton(70, 890, 32, 32, getName("GenerateCreatures"), getName("GenerateCreaturesPrompt"), false,  SimpleColor.DIRTY_WHITE, "WorldGeneratorBasicSwap");
        createSwapButton(70, 820, 32, 32, getName("RandomSpawn"), getName("RandomSpawnPrompt"), false, SimpleColor.DIRTY_WHITE, "WorldGeneratorBasicSwap");
    }

    private static void createGenerationSet() {
        createSwapButton(70, 890, 32, 32, getName("GenerateSimpleWorld"), getName("GenerateSimpleWorldPrompt"), false, SimpleColor.DIRTY_WHITE, "WorldGeneratorGenerationSwap");
    }

    private static void createPhysicsSet() {

    }

    private static void deleteBasicSet() {
        buttons.values().stream().filter(button -> button.group.equals("WorldGeneratorBasicSwap")).forEach(button -> button.visible = false);
    }

    private static void deleteGenerationSet() {
        buttons.values().stream().filter(button -> button.group.equals("WorldGeneratorGenerationSwap")).forEach(button -> button.visible = false);
    }

    private static void deletePhysicsSet() {
        buttons.values().stream().filter(button -> button.group.equals("WorldGeneratorPhysicsSwap")).forEach(button -> button.visible = false);
    }
}
