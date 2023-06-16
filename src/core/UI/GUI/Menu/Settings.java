package core.UI.GUI.Menu;

import core.EventHandling.Logging.Json;
import java.awt.*;
import static core.EventHandling.Logging.Config.getFromConfig;
import static core.EventHandling.Logging.Config.updateConfig;
import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;

public class Settings {
    public static boolean createdSettings = false, needUpdateCount = true;
    public static int pressedCount = 0;

    public static void create() {
        createPanel(20, 20, 1880, 1040, "defaultPanSettings", false);
        createPanel(40, 40, 240, 1000, "leftPanSettings", true);

        createButton(40, 900, 240, 65, getName("SettingsExit"), null, true, new Color(255, 80, 0, 55));
        createButton(40, 800, 240, 65, getName("SettingsSave"), null, true, new Color(255, 80, 0, 55));

        createButton(40, 300, 240, 65, getName("SettingsGraphics"), null, true, new Color(0, 0, 0, 50));
        createButton(40, 200, 240, 65, getName("SettingsBasic"), null, true, new Color(0, 0, 0, 50));
        createButton(40, 100, 240, 65, getName("SettingsOther"), null, true, new Color(0, 0, 0, 50));
        buttons.get(Json.getName("SettingsSave")).isClickable = false;

        createdSettings = true;
        createGraphicsSett();
    }

    public static void createGraphicsSett() {
        createSwapButton(310, 980, 32, 32, getName("InterpolateSunset"), getName("InterpolateSunsetPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("InterpolateSunset")));
        createSwapButton(310, 910, 32, 32, getName("PreloadTextures"), getName("PreloadTexturesPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("PreloadTextures")));
        createSwapButton(310, 840, 32, 32, getName("VerticalSync"), getName("VerticalSyncPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("VerticalSync")));
    }


    public static void createBasicSett() {
        createSwapButton(310, 980, 32, 32, getName("ShowPrompts"), getName("ShowPromptsPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("InterpolateSunset")));
    }

    public static void createOtherSett() {

    }

    public static void deleteGraphicsSett() {
        buttons.get(getName("InterpolateSunset")).visible = false;
        buttons.get(getName("PreloadTextures")).visible = false;
        buttons.get(getName("VerticalSync")).visible = false;
    }

    public static void deleteBasicSett() {
        buttons.get(getName("ShowPrompts")).visible = false;
    }

    public static void deleteOtherSett() {

    }

    public static void delete() {
        panels.get("defaultPanSettings").visible = false;
        panels.get("leftPanSettings").visible = false;

        buttons.get(getName("SettingsExit")).visible = false;
        buttons.get(getName("SettingsSave")).visible = false;
        buttons.get(getName("SettingsGraphics")).visible = false;
        buttons.get(getName("SettingsBasic")).visible = false;
        buttons.get(getName("SettingsOther")).visible = false;

        deleteGraphicsSett();
        deleteBasicSett();
        deleteOtherSett();
        createdSettings = false;
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
