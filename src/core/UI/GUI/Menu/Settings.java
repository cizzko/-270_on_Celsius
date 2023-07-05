package core.UI.GUI.Menu;

import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Json;
import java.awt.*;
import static core.EventHandling.Logging.Config.getFromConfig;
import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;

public class Settings {
    public static boolean createdSettings = false, needUpdateCount = true;
    public static int pressedCount = 0;

    public static void create() {
        createPanel(20, 20, 1880, 1040, "defaultPanSettings", false, "Settings");
        createPanel(40, 40, 240, 1000, "leftPanSettings", true, "Settings");

        createButton(40, 900, 240, 65, getName("SettingsExit"), null, true, new Color(255, 80, 0, 55), "Settings");
        createButton(40, 800, 240, 65, getName("SettingsSave"), null, true, new Color(255, 80, 0, 55), "Settings");

        createButton(40, 300, 240, 65, getName("SettingsGraphics"), null, true, new Color(0, 0, 0, 50), "Settings");
        createButton(40, 200, 240, 65, getName("SettingsBasic"), null, true, new Color(0, 0, 0, 50), "Settings");
        createButton(40, 100, 240, 65, getName("SettingsOther"), null, true, new Color(0, 0, 0, 50), "Settings");
        buttons.get(Json.getName("SettingsSave")).isClickable = false;
        buttons.get(getName("SettingsGraphics")).isClickable = false;

        createdSettings = true;
        createGraphicsSett();
    }

    public static void createGraphicsSett() {
        createSwapButton(310, 980, 32, 32, getName("InterpolateSunset"), getName("InterpolateSunsetPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("InterpolateSunset")), "SettingsGraphicsSwap");
        createSwapButton(310, 910, 32, 32, getName("PreloadTextures"), getName("PreloadTexturesPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("PreloadTextures")), "SettingsGraphicsSwap");
        createSwapButton(310, 840, 32, 32, getName("VerticalSync"), getName("VerticalSyncPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("VerticalSync")), "SettingsGraphicsSwap");
    }


    public static void createBasicSett() {
        createSwapButton(310, 980, 32, 32, getName("ShowPrompts"), getName("ShowPromptsPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("ShowPrompts")), "SettingsBasicSwap");
        createSwapButton(310, 910, 32, 32, getName("DetectLanguage"), getName("DetectLanguagePrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("DetectLanguage")), "SettingsBasicSwap");
    }

    public static void createOtherSett() {
        createSwapButton(310, 980, 32, 32, getName("SendAnonymousStatistics"), getName("SendAnonymousStatisticsPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("SendAnonymousStatistics")), "SettingsOtherSwap");
    }

    public static void deleteGraphicsSett() {
        buttons.values().stream().filter(button -> button.group.contains("SettingsGraphics")).forEach(button -> button.visible = false);
    }

    public static void deleteBasicSett() {
        buttons.values().stream().filter(button -> button.group.contains("SettingsBasic")).forEach(button -> button.visible = false);
    }

    public static void deleteOtherSett() {
        buttons.values().stream().filter(button -> button.group.contains("SettingsOther")).forEach(button -> button.visible = false);
    }

    public static void delete() {
        panels.values().stream().filter(button -> button.group.equals("Settings")).forEach(button -> button.visible = false);
        buttons.values().stream().filter(button -> button.group.equals("Settings")).forEach(button -> button.visible = false);

        deleteGraphicsSett();
        deleteBasicSett();
        deleteOtherSett();
        createdSettings = false;
    }

    public static void updateConfigAll() {
        buttons.values().stream().filter(button -> button.group.contains("Swap")).forEach(button -> Config.updateConfig(Json.getKey(button.name), String.valueOf(button.isClicked)));
    }
}
