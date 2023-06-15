package core.UI.GUI.Menu;

import java.awt.*;
import static core.EventHandling.Logging.Config.getFromConfig;
import static core.EventHandling.Logging.Config.updateConfig;
import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;

public class Settings {
    public static boolean createdGraphics = false, createdBasic = false, createdOther = false;

    public static void create() {
        createPanel(20, 20, 1880, 1040, "defaultPanSettings", false);
        createPanel(40, 40, 240, 1000, "leftPanSettings", true);

        createButton(40, 915, 240, 65, getName("SettingsExit"), null, true, new Color(255, 80, 0, 55));
        createButton(40, 300, 240, 65, getName("SettingsGraphics"), null, true, new Color(0, 0, 0, 50));
        createButton(40, 203, 240, 65, getName("SettingsBasic"), null, true, new Color(0, 0, 0, 50));
        createButton(40, 105, 240, 65, getName("SettingsOther"), null, true, new Color(0, 0, 0, 50));
        createGraphicsSett();
    }

    public static void createGraphicsSett() {
        if (!createdGraphics) {
            createSwapButton(310, 980, 32, 32, getName("InterpolateSunset"), getName("InterpolateSunsetPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("InterpolateSunset")));
            createSwapButton(310, 910, 32, 32, getName("PreloadTextures"), getName("PreloadTexturesPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("PreloadTextures")));
            createSwapButton(310, 840, 32, 32, getName("VerticalSync"), getName("VerticalSyncPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("VerticalSync")));
            createdGraphics = true;
        }
    }

    public static void createBasicSett() {
        if (!createdBasic) {
            createSwapButton(310, 980, 32, 32, getName("ShowPrompts"), getName("ShowPromptsPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("InterpolateSunset")));
            createdBasic = true;
        }
    }

    public static void createOtherSett() {
        if (!createdOther) {
            createdOther = true;
        }
    }

    public static void deleteGraphicsSett() {
        if (createdGraphics) {
            buttons.get(getName("InterpolateSunset")).visible = false;
            buttons.get(getName("PreloadTextures")).visible = false;
            buttons.get(getName("VerticalSync")).visible = false;
            createdGraphics = false;
        }
    }

    public static void deleteBasicSett() {
        if (createdBasic) {
            buttons.get(getName("ShowPrompts")).visible = false;
            createdBasic = false;
        }
    }

    public static void deleteOtherSett() {
        if (createdOther) {
            createdOther = false;
        }
    }

    public static void delete() {
        panels.get("defaultPanSettings").visible = false;
        panels.get("leftPanSettings").visible = false;

        buttons.get(getName("SettingsExit")).visible = false;
        buttons.get(getName("SettingsGraphics")).visible = false;
        buttons.get(getName("SettingsBasic")).visible = false;
        buttons.get(getName("SettingsOther")).visible = false;

        deleteGraphicsSett();
        deleteBasicSett();
        deleteOtherSett();
    }

    public static void updateConfigAll() {
        try {
            updateConfig("InterpolateSunset", String.valueOf(buttons.get(getName("InterpolateSunset")).isClicked));
            updateConfig("PreloadTextures", String.valueOf(buttons.get(getName("PreloadTextures")).isClicked));
            updateConfig("VerticalSync", String.valueOf(buttons.get(getName("VerticalSync")).isClicked));
            updateConfig("ShowPrompts", String.valueOf(buttons.get(getName("ShowPrompts")).isClicked));
        } catch (Exception e) { /*нечего обрабатывать*/ }
    }
}
